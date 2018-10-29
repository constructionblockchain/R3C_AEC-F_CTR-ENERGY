package net.cordaclub.p2penergy

import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.contextLogger
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

}

class P2PEnergyWebPlugin : WebServerPluginRegistry {
    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::P2PEnergyApi))

    override val staticServeDirs: Map<String, String> = mapOf(
            "neighbour" to javaClass.classLoader.getResource("neighbourWeb").toExternalForm(),
            "powerplant" to javaClass.classLoader.getResource("powerPlantWeb").toExternalForm()
    )
}