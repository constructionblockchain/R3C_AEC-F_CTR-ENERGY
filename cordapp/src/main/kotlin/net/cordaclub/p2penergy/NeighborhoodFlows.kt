package net.cordaclub.p2penergy

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction

object NeighborhoodFlows {

    /**
     * This flow will be triggered on the Coordinator's Node
     */
    @InitiatingFlow
    class EnergyNettingFlow(val lastInterval: Int, val neighbors: List<Party>, val utilityCompany: Party) : FlowLogic<SignedTransaction>() {
        override fun call(): SignedTransaction {

            // TODO: select my (coordinator) state from last interval
            // Create transaction builder and attach above state
            // send to each participant to attach their state
            // Now we have all states from everyone we run the netting function
            // based on the resulted map send payment requests to other neighbours
            // after the cash states are attached, sign and send to everyone to sign
            TODO("Implement me")
        }

    }

    /**
     * This flow will respond to messages from the coordinator.
     * and will run on each other participants
     *
     * Todo - slightly different behaviour on the UtilityNode - it will just to the last step
     */
    @InitiatedBy(EnergyNettingFlow::class)
    class EnergyNettingResponseFlow(private val session: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            // receive request to attach energy state
            // attach it and send back

            // receive netting result and respond accordingly - by attaching cash

            // receive final transaction and verify netting is correct and all states are good

            // sign and send back

            TODO("Implement me")
        }
    }
}