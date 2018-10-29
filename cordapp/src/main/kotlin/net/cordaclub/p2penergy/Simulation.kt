package net.cordaclub.p2penergy

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.seconds
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.timerTask

val powerPlantPort = 10004
val neighborPorts = listOf(10008, 10012)

val simulationInterval = 5.seconds.seconds

// todo - issue cash
fun main(args: Array<String>) {
    CordaRPCClient(NetworkHostAndPort("localhost", powerPlantPort)).use("user1", "test") { conn ->
//        fixedRateTimer(initialDelay = 10000, period = simulationInterval * 1000, action = {
//            println("power")
//        } )
    }

    neighborPorts.forEach { neighborPort ->
        CordaRPCClient(NetworkHostAndPort("localhost", neighborPort)).use("user1", "test") { conn ->
            fixedRateTimer(initialDelay = 10000, period = simulationInterval * 1000, action = {
                println("neighbour ")
            } )
        }
    }
}