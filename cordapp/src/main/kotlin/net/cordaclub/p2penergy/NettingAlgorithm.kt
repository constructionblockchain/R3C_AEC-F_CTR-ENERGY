package net.cordaclub.p2penergy

import net.corda.core.identity.Party

//data class NettingResult(val amountToPay: Amount<Currency>?, val amountToReceive: Amount<Currency>?)
data class NettingResult(val amountToPay: Int?, val amountToReceive: Int?)

// pricing
fun netting(readings: List<EnergyEventState>, utilityCompany: Party): Map<Party, NettingResult> {
//TODO
// 1) sum up all readings (reading =end-start)
// 2) if sum >0 - participants who produced need to receive, etc
// 3) calculate what is owed (what it owes) to the Utility and what to the other neighbours
// 4) calculate weighted averages and based on the pricing calculate what each party owes or is owed
// 5) return a map of Party - NettingResult
    TODO("not implemented")
}