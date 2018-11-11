package net.cordaclub.p2penergy

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Issued
import net.corda.core.identity.Party
import net.corda.core.internal.sum
import net.corda.core.serialization.CordaSerializable
import net.corda.finance.`issued by`
import net.cordaclub.p2penergy.Pricing.p2pPrice
import net.cordaclub.p2penergy.Pricing.utilityBuyPrice
import net.cordaclub.p2penergy.Pricing.utilitySellPrice
import java.math.BigDecimal
import java.util.*

private data class NettingResultInternal(val amountToPay: BigDecimal? = null, val amountToReceive: BigDecimal? = null) {
    override fun toString(): String = if (amountToPay != null) "${-amountToPay}" else "$amountToReceive"
}
@Suspendable
fun List<Amount<Issued<Currency>>>.sum() = this.fold(Amount.zero(this.first().token)) { acc, e -> acc + e }

@Suspendable
fun List<BigDecimal>.sum() = this.fold(BigDecimal.ZERO) { acc, e -> acc + e }

@CordaSerializable
data class NettingResult(val amountToPay: Amount<Issued<Currency>>?, val amountToReceive: Amount<Issued<Currency>>?)

// receives a map of PartyName-readingValue
// The readingValue is positive if the party produced, and negative if he consumed
@Suspendable
private fun <T> nettingInternal(readings: Map<T, Int>, utilityCompany: T): Map<T, NettingResultInternal> {

    // Sum up all readings readings (reading = end-start).
    val sumEnergyReadings = readings.values.sumBy { it }

    val positiveReadings = readings.filter { it.value >= 0 }
    val negativeReadings = readings.filter { it.value < 0 }.map { it.key to -it.value }.toMap()

    // Select the participants who only deal with the other participants, based on the total input/output of the system.
    val onlyP2PParticipants = if (sumEnergyReadings > 0) negativeReadings else positiveReadings

    // Calculate what each participant owes/is owned inside the pool by multiplying p2p price to consumption.
    val perParticipantP2PCost = onlyP2PParticipants.map { (party, reading) -> party to BigDecimal(p2pPrice * reading) }.toMap()

    // This is sum of all money changing hands between participants.
    val totalP2PCost = perParticipantP2PCost.values.sum()

    // Then handle participants who have to deal with the Energy company.
    val otherParticipants = if (sumEnergyReadings > 0) positiveReadings else negativeReadings
    val sumOtherReadings = otherParticipants.values.sumBy { it }

    val utilityPrice = if (sumEnergyReadings > 0) utilityBuyPrice else -utilitySellPrice

    // Total amount of money leaving/entering the system.
    val totalUtilityCost = BigDecimal(sumEnergyReadings * utilityPrice)

    // Total amount of money that will change hands.
    val totalMoneyInTheSystem = totalP2PCost + totalUtilityCost

    // Calculate a weighted average for all participants that deal with the utility to determine their share.
    val perParticipantUtilityCost = otherParticipants.map { (party, reading) ->
        party to (BigDecimal(reading) * totalMoneyInTheSystem / BigDecimal(sumOtherReadings))
    }.toMap()

    // balance out
    val total = perParticipantUtilityCost.values.sum() - perParticipantP2PCost.values.sum()
    assert(total == totalUtilityCost)

    var result = perParticipantP2PCost.map { (k, v) -> k to if (sumEnergyReadings > 0) NettingResultInternal(amountToPay = v) else NettingResultInternal(amountToReceive = v) }.toMap() +
            perParticipantUtilityCost.map { (k, v) -> k to if (sumEnergyReadings > 0) NettingResultInternal(amountToReceive = v) else NettingResultInternal(amountToPay = v) }.toMap() +
            listOf(utilityCompany to if (sumEnergyReadings > 0) NettingResultInternal(amountToPay = totalUtilityCost) else NettingResultInternal(amountToReceive = totalUtilityCost)).toMap()

    result = result.map { (party, netting) ->
        party to netting.copy(amountToPay = netting.amountToPay?.div(BigDecimal.valueOf(100L)), amountToReceive = netting.amountToReceive?.div(BigDecimal.valueOf(100L)))
    }.toMap()

    val roundingError = result.mapNotNull { it.value.amountToPay }.sum() - result.mapNotNull { it.value.amountToReceive }.sum()

    return result.map { (party, netting) ->
        party to if (party == utilityCompany) {
            if (netting.amountToPay !== null)
                netting.copy(amountToPay = netting.amountToPay - roundingError)
            else
                netting.copy(amountToReceive = netting.amountToReceive!! + roundingError)
        } else {
            netting
        }
    }.toMap()
}

@Suspendable
fun netStates(states: List<EnergyEventState>, utilityCompany: Party, issuer: Party, currency: Currency): Map<Party, NettingResult> {
    fun toAmount(amt: BigDecimal) = Amount.fromDecimal(amt, currency.`issued by`(issuer.ref(1.toByte())))

    val internalResult = nettingInternal(states.map { it.household to (it.readingEnd - it.readingStart) }.toMap(), utilityCompany)

    return internalResult.map { (party, netting) ->
        party to NettingResult(netting.amountToPay?.let(::toAmount), netting.amountToReceive?.let(::toAmount))
    }.toMap()
}

fun main(args: Array<String>) {
    fun generateRandomConsumption(min: Int, max: Int): Int = Random().nextInt(max - min + 1) + min

    val allNeighbours = listOf("a", "b", "c", "d", "e")

    var energyReadings = allNeighbours.map { it to 0 }

    for (interval in (1..100)) {
        println("------------")
        val original = energyReadings.toList()

        energyReadings = energyReadings.map { (party, reading) ->
            val finalReading = reading + generateRandomConsumption(-500, 500)
            party to finalReading
        }

        println("Iteration: $interval. Readings: ${energyReadings.map { it.second }}")

        val result = nettingInternal(
                energyReadings.zip(original).map { (end, st) -> end.first to (end.second - st.second) }.toMap(), "f")
        println(result.toSortedMap())

        val toPay = result.mapNotNull { it.value.amountToPay }.sum()
        val toRec = result.mapNotNull { it.value.amountToReceive }.sum()
        println("${toPay - toRec}")
    }
}