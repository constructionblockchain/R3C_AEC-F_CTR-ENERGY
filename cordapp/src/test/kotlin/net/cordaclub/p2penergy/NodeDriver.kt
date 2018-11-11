package net.cordaclub.p2penergy

import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.finance.POUNDS
import net.corda.finance.flows.CashIssueAndPaymentFlow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.User
import java.lang.Thread.sleep
import java.util.*

fun main(args: Array<String>) {
    val issuedMoney = 100.POUNDS
    val issuerBankPartyRef = OpaqueBytes.of(1)

    val user = User("user1", "test", permissions = setOf("ALL"))
    driver(DriverParameters(
            extraCordappPackagesToScan = listOf("net.cordaclub.p2penergy", "net.corda.finance.contracts.asset"),
            waitForAllNodesToFinish = true,
            startNodesInProcess = false,
            isDebug = false
    )) {
        val partyA = startNode(providedName = CordaX500Name("John's House", "London", "GB"), rpcUsers = listOf(user)).getOrThrow()
        val partyB = startNode(providedName = CordaX500Name("Mary's House", "London", "GB"), rpcUsers = listOf(user)).getOrThrow()
        val partyC = startNode(providedName = CordaX500Name("Jane's House", "London", "GB"), rpcUsers = listOf(user)).getOrThrow()
//        val partyD = startNode(providedName = CordaX500Name("Jack's House", "London", "GB"), rpcUsers = listOf(user)).getOrThrow()
//        val partyE = startNode(providedName = CordaX500Name("Alice's House", "London", "GB"), rpcUsers = listOf(user)).getOrThrow()
        val bank = startNode(providedName = CordaX500Name("Bank", "London", "GB"), rpcUsers = listOf(user)).getOrThrow()
        val utility = startNode(providedName = CordaX500Name("Utility", "London", "GB"), rpcUsers = listOf(user)).getOrThrow()
        startWebserver(partyA)
        startWebserver(partyB)
        startWebserver(partyC)
        startWebserver(utility)

//        val allNeighbours = listOf(partyA, partyB, partyC, partyD, partyE)
        val allNeighbours = listOf(partyA, partyB, partyC)
        val notaryLegalIdentity = notaryHandles.first().identity

        // Issue Cash to everyone
        for (nodeHandle in (allNeighbours + utility)) {
            bank.rpc.startFlow(::CashIssueAndPaymentFlow,
                    CashIssueAndPaymentFlow.IssueAndPaymentRequest(
                            issuedMoney, issuerBankPartyRef, nodeHandle.nodeInfo.legalIdentities.first(), notaryLegalIdentity, false)
            ).returnValue.getOrThrow()
        }

        var energyReadings = allNeighbours.map { it to 0 }

        for (interval in (1..100)) {
            println("------------")
            println("Iteration: $interval. Readings: ${energyReadings.map { it.second }}")
            energyReadings = energyReadings.map { (party, reading) ->
                val consumption = generateRandomConsumption(-50, 50)
                val finalReading = reading + consumption
                party.rpc.startFlow(EnergyFlows::RegisterEnergyConsumption, interval, reading, finalReading).returnValue.getOrThrow()
                party to finalReading
            }

            val tx = partyA.rpc.startFlow(EnergyFlows::EnergyNettingFlow, interval, allNeighbours.drop(1).map { it.nodeInfo.legalIdentities.first() }, utility.nodeInfo.legalIdentities.first(), bank.nodeInfo.legalIdentities.first()).returnValue.getOrThrow()
            println(tx.coreTransaction)
//            sleep(1000)
        }

    }
}

val r = Random()
fun generateRandomConsumption(min: Int, max: Int): Int = r.nextInt(max - min + 1) + min
