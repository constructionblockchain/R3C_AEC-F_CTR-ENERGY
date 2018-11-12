package net.cordaclub.p2penergy

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import net.corda.finance.GBP
import java.util.*

object Pricing {
    const val p2pPrice = 9 // this is 9p per kwh
    const val utilitySellPrice = 14
    const val utilityBuyPrice = 14
    val energyCurrency = GBP
}

@CordaSerializable
enum class EnergyStateStatus { CREATED, TRADED }

/**
 * This is the main fact that lives on the ledger.
 * This state will be published every 30 minutes by the smart meter, which is tamper proof.
 *
 * The actual energy is: readingEnd - readingStart.
 */
data class EnergyEventState(
        val party: Party,
        val interval: Int,  // 1.1.2019: 00:00-29 = 1, 00:30-01:00 = 2
        val readingStart: Int, // in kwh
        val readingEnd: Int, // in kwh
        val tradedStatus: EnergyStateStatus,
        val payed: Amount<Issued<Currency>>? = null,
        val received: Amount<Issued<Currency>>? = null
) : ContractState {

    override val participants: List<AbstractParty>
        get() = listOf(party)
}

class EnergyEventContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        // TODO - check that all input states are CREATED and have an equivalent TRADED output.
        // TODO - check that all parties actually payed or received what they declare in the state.
    }
}

// Used to indicate the transaction's intent.
interface Commands : CommandData {
    class Consume : Commands
    class Trade : Commands
}

// TODO - create contract that runs the netting and checks it is correct.
//class ReadingReconciliationState(){}
