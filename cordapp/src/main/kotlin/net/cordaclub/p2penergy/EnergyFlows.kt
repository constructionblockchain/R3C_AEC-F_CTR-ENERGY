package net.cordaclub.p2penergy

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.asset.cash.selection.AbstractCashSelection
import net.cordaclub.p2penergy.Pricing.energyCurrency
import java.security.PublicKey
import java.util.*

object EnergyFlows {

    /**
     * Called each interval to register the consumption.
     * This creates an energy state.
     */
    @StartableByRPC
    class RegisterEnergyConsumption(private val interval: Int, private val readingStart: Int, private val readingEnd: Int) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val builder = TransactionBuilder(notary)
            val state = EnergyEventState(ourIdentity, interval, readingStart, readingEnd, EnergyStateStatus.CREATED)
            builder.addOutputState(state, EnergyEventContract::class.java.name)
            builder.addCommand(Commands.Consume(), ourIdentity.owningKey)
            val tx = serviceHub.signInitialTransaction(builder, ourIdentity.owningKey)
            return subFlow(FinalityFlow(tx, emptySet()))
        }
    }

    /**
     * This flow will be triggered on the Coordinator's Node
     */
    @StartableByRPC
    @InitiatingFlow
    class EnergyNettingFlow(private val lastInterval: Int, private val neighbours: List<Party>, private val utilityCompany: Party, private val bank: Party) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.first()

            // select my state from last interval.  TODO -inefficient
            val state = serviceHub.vaultService.queryBy(EnergyEventState::class.java).states.find { it.state.data.interval == lastInterval }!!

            // Create transaction builder and attach above state
            var builder = TransactionBuilder(notary = notary)
                    .addInputState(state)
                    .addCommand(Commands.Trade(), neighbours.map { it.owningKey } + utilityCompany.owningKey)

            val sessions = (neighbours + utilityCompany).map { neighbour ->
                neighbour to initiateFlow(neighbour)
            }.toMap()
            // Request the energy state from all neighbours.
            val energyStates = sessions.map { (_, session) ->
                session.send(lastInterval)
                val neighbourState = subFlow(ReceiveStateAndRefFlow<EnergyEventState>(session))
                if (neighbourState.firstOrNull() != null)
                    builder = builder.addInputState(neighbourState.single())
                neighbourState.firstOrNull()
            }

            logger.warn("ENERGY: " + energyStates.map { it?.state?.data?.household } + " = " + energyStates.map { it?.state?.data?.readingEnd })

            // Now we have all states from everyone we run the netting function
            val nettingResults = netStates(
                    energyStates.filter { it != null }.map { it!!.state.data } + state.state.data, utilityCompany, bank, energyCurrency)

            val cashOwners = mutableSetOf<PublicKey>()

            // If we have to pay anything, select cash and add to the transaction.
            val netting = nettingResults[ourIdentity]!!.amountToPay
            if (netting != null) {
                val cashSelection = AbstractCashSelection.getInstance { serviceHub.jdbcSession().metaData }
                val myCash = cashSelection.unconsumedCashStatesForSpending(serviceHub, netting.withoutIssuer(), lockId = UUID.randomUUID())
                myCash.forEach {
                    cashOwners.add(it.state.data.owner.owningKey)
                    builder = builder.addInputState(it)
                }

                var total = Amount.zero(netting.token)
                myCash.forEach { c -> total = total.plus(c.state.data.amount) }
                val change = total.minus(netting)
                if (change.quantity > 0) {
                    builder = builder.addOutputState(Cash.State(change, ourIdentity), Cash.PROGRAM_ID)
                }
            }

            // based on the resulted map send payment requests to other neighbours
            sessions.forEach { (party, session) ->
                val nettingResult = nettingResults[party]!!
                session.send(nettingResult)
                if (nettingResult.amountToPay != null) {
                    val states = subFlow(ReceiveStateAndRefFlow<Cash.State>(session))

                    for (cash in states) {
                        cashOwners.add(cash.state.data.owner.owningKey)
                        builder = builder.addInputState(cash)
                    }

                    val change = session.receive<Amount<Issued<Currency>>>().unwrap { it }
                    if (change.quantity > 0) {
                        builder = builder.addOutputState(Cash.State(change, party), Cash.PROGRAM_ID)
                    }
                }
            }

            // Create the traded energy states.
            (energyStates + state).filter { it != null }.forEach { energyState ->
                builder = builder.addOutputState(energyState!!.state.data.copy(
                        tradedStatus = EnergyStateStatus.TRADED,
                        payed = nettingResults[energyState.state.data.household]?.amountToPay,
                        received = nettingResults[energyState.state.data.household]?.amountToReceive),
                        EnergyEventContract::class.java.name)
            }

            // Add the output payments - for the parties that receive money
            nettingResults.filter { it.value.amountToReceive != null }.forEach { (party, nettingResult) ->
                if (nettingResult.amountToReceive!!.quantity > 0) {
                    builder = builder.addOutputState(Cash.State(nettingResult.amountToReceive!!, party), Cash.PROGRAM_ID)
                }
            }

            builder = builder.addCommand(Cash.Commands.Move(), cashOwners.toList())
            logger.warn(builder.toLedgerTransaction(serviceHub).toString())

            // after the cash states are attached, sign and send to everyone to sign
            val tx = serviceHub.signInitialTransaction(builder, ourIdentity.owningKey)

            val signedTx = subFlow(CollectSignaturesFlow(tx, sessions.map { it.value }))
            return subFlow(FinalityFlow(signedTx, emptySet()))
        }
    }

    /**
     * This flow will respond to messages from the coordinator.
     * and will run on each other participants
     *
     * Slightly different behaviour on the UtilityNode as it does not have to send any
     */
    @InitiatedBy(EnergyNettingFlow::class)
    class EnergyNettingResponseFlow(private val session: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val isUtility = ourIdentity.name.organisation == "Utility"

            // receive request to attach energy state
            val lastInterval = session.receive<Int>().unwrap { it }

            val toSend = if (!isUtility)
                listOf(serviceHub.vaultService.queryBy(EnergyEventState::class.java).states.find { it.state.data.interval == lastInterval }!!)
            else emptyList()

            // attach it and send back
            subFlow(SendStateAndRefFlow(session, toSend))

            // receive netting result and respond accordingly - by attaching cash
            val netting = session.receive<NettingResult>().unwrap { it }
            if (netting.amountToPay != null) {
                val cashSelection = AbstractCashSelection.getInstance { serviceHub.jdbcSession().metaData }
                val amount = netting.amountToPay!!
                val cash: List<StateAndRef<Cash.State>> = cashSelection.unconsumedCashStatesForSpending(serviceHub, amount.withoutIssuer(), lockId = UUID.randomUUID())

                var total = Amount.zero(amount.token)
                cash.forEach { c -> total = total.plus(c.state.data.amount) }
                val change = total.minus(amount)

                subFlow(SendStateAndRefFlow(session, cash))
                session.send(change)
            }

            // receive final transaction and verify netting is correct and all states are good
            // sign and send back
            val tx = subFlow(object : SignTransactionFlow(session) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                }
            })

            return waitForLedgerCommit(tx.id)
        }
    }
}