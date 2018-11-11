package net.cordaclub.p2penergy

import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.contextLogger
import net.corda.finance.contracts.asset.Cash
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

// Accessible at /api/p2penergy/..
@Path("p2penergy")
class P2PEnergyApi(val rpcOps: CordaRPCOps) {

    companion object {
        val logger = contextLogger()
    }

    val myLegalName = rpcOps.nodeInfo().legalIdentities[0].name

    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun myIdentity() = Response.ok(mapOf("me" to myLegalName)).build()

    @GET
    @Path("energy")
    @Produces(MediaType.APPLICATION_JSON)
    fun energyStates()= rpcOps.vaultQuery(EnergyEventState::class.java).states

    @GET
    @Path("balance")
    @Produces(MediaType.APPLICATION_JSON)
    fun balance(): Response {
        return Response.ok(mapOf("balance" to rpcOps.vaultQuery(Cash.State::class.java).states.filter { it.state.data.owner.nameOrNull() == myLegalName }.map { it.state.data.amount }.sum())).build()
    }
}

class P2PEnergyWebPlugin : WebServerPluginRegistry {
    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::P2PEnergyApi))

    override val staticServeDirs: Map<String, String> = mapOf(
            "neighbour" to javaClass.classLoader.getResource("neighbourWeb").toExternalForm(),
            "powerplant" to javaClass.classLoader.getResource("powerPlantWeb").toExternalForm()
    )
}