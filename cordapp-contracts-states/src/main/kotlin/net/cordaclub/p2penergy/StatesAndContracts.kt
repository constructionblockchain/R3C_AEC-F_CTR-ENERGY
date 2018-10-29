package net.cordaclub.p2penergy

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction

enum class EventType { DEMAND_ENERGY_UNIT, SURPLUS_ENERGY_UNIT }

data class Reading(val value:Int){}

class EnergyEventState(
        val houseHold: Party,
        val eventType: EventType,
        val interval: Int,  // 1.1.2019: 00:00-29 = 1, 00:30-01:00 = 2

        val reading: Reading,

        val traded: Boolean
) : ContractState {
    override val participants: List<AbstractParty>
        get() = listOf(houseHold)
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


class ReadingReconciliationState(){} //stuff

