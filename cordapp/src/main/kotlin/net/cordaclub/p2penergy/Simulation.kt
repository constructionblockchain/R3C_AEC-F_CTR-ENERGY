package net.cordaclub.p2penergy

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.seconds
import net.corda.finance.POUNDS
import net.corda.finance.flows.CashIssueAndPaymentFlow
import net.cordaclub.p2penergy.EnergyFlows.EnergyNettingFlow
import net.cordaclub.p2penergy.EnergyFlows.RegisterEnergyConsumption
import java.util.*
import kotlin.concurrent.fixedRateTimer

val powerPlant = CordaX500Name("PowerPlant", "London", "GB") to 10004
val bank = CordaX500Name("NeighbourhoodBank", "London", "GB") to 10016
val neighbors = mapOf(
        CordaX500Name("NeighbourOne", "London", "GB") to 10008,
        CordaX500Name("NeighbourTwo", "London", "GB") to 10012)

val simulationInterval = 5.seconds.seconds * 1000

val issuedMoney = 100.POUNDS
val issuerBankPartyRef = OpaqueBytes.of(1)

fun main(args: Array<String>) {

    // Issue cash
    CordaRPCClient(NetworkHostAndPort("localhost", bank.second)).use("user1", "test") { conn ->
        val rpc = conn.proxy
        rpc.waitUntilNetworkReady().getOrThrow()
        val notaryLegalIdentity = rpc.notaryIdentities().first()

        // Issue money to all parties
        (neighbors.keys + powerPlant.first).forEach { party ->
            // Resolve parties via RPC
            val issueToParty = rpc.wellKnownPartyFromX500Name(party)
            val issueRequest = CashIssueAndPaymentFlow.IssueAndPaymentRequest(issuedMoney, issuerBankPartyRef, issueToParty!!, notaryLegalIdentity, false)
            val tx = rpc.startFlow(::CashIssueAndPaymentFlow, issueRequest).returnValue.getOrThrow().stx
            println("Issued $issuedMoney to $party - ${tx.id}.")
        }
    }

    var currentInterval = 1
    neighbors.forEach { (name, port) ->
        CordaRPCClient(NetworkHostAndPort("localhost", port)).use("user1", "test") { conn ->
//            fixedRateTimer(initialDelay = 10000, period = simulationInterval, action = {
                val rpc = conn.proxy
                val consumed = generateRandomConsumption()
//                println("${rpc.nodeInfo().legalIdentities.first()} consumed $consumed")

                // Todo - handle interval and readings
                rpc.startFlow(::RegisterEnergyConsumption, currentInterval, 10, 20).returnValue

                if (name.organisation == "NeighbourOne") {
                    rpc.startFlow(::EnergyNettingFlow, currentInterval, neighbors.keys.map{rpc.wellKnownPartyFromX500Name(it)!!}, rpc.wellKnownPartyFromX500Name(powerPlant.first)!!, rpc.wellKnownPartyFromX500Name(bank.first)!!).returnValue
                }
//            })
        }
    }

//    CordaRPCClient(NetworkHostAndPort("localhost", powerPlant.second)).use("user1", "test") { conn ->
//    }

}

fun generateRandomConsumption(): Int = Random().nextInt()
