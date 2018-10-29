package net.cordaclub.p2penergy

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction

object Pricing {
    val p2pPrice = 9 // this is 9p
    val utilitySell = 14
    val utilityBuy = 14

}

/**
 * This is the main fact that lives on the ledger.
 * This state will be published every 30 minutes by the smart meter, which is tamper proof.
 *
 * The actual energy is: readingEnd - readingStart
 *
 */
class EnergyEventState(
        val household: Party,
        val interval: Int,  // 1.1.2019: 00:00-29 = 1, 00:30-01:00 = 2
        val readingStart: Int, // in Watts
        val readingEnd: Int, // in Watts
        val tradedStatus: Boolean
) : ContractState {
    override val participants: List<AbstractParty>
        get() = listOf(household)
}

class EnergyEventContract : Contract {
    companion object {
        val ID = "com.p2penergy.EnergyEventContract"
    }

    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Action : Commands
    }
}


//class ReadingReconciliationState(){} //stuff

