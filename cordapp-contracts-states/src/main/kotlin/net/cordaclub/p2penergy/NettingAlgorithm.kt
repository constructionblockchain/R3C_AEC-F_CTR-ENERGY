package net.cordaclub.p2penergy

import net.corda.core.identity.Party

//data class NettingResult(val amountToPay: Amount<Currency>?, val amountToReceive: Amount<Currency>?)
data class NettingResult(val amountToPay: Int? = null, val amountToReceive: Int? = null)

// pricing
// receives a map of PartyName-readingValue
// The readingValue is positive if the party produced, and negative if he consumed
fun netting(readings: Map<String, Int>, utilityCompany: String): Map<String, NettingResult> {
//TODO
// 1) sum up all readings (reading =end-start)
// 2) if sum >0 - participants who produced need to receive, etc
// 3) calculate what is owed (what it owes) to the Utility and what to the other neighbours
// 4) calculate weighted averages and based on the pricing calculate what each party owes or is owed
// 5) return a map of Party - NettingResult
//    TODO("not implemented")
    return mapOf(
            utilityCompany to NettingResult(amountToReceive = 10),
            "Alice" to NettingResult(amountToPay = 20)
    )
}


fun main(args: Array<String>) {

    val readings = mapOf("Alice" to 400, "Bob" to -300, "Charlie" to 1000, "John" to -200)

    val result = netting(readings, "MegaUtilityCompany")

    println(result)

}