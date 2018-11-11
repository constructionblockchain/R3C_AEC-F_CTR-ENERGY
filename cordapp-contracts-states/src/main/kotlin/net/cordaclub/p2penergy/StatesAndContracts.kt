package net.cordaclub.p2penergy

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import net.corda.finance.GBP
import java.util.*
import javax.persistence.Entity
import javax.persistence.Table

object Pricing {
    val p2pPrice = 9 // this is 9p per kwh
    val utilitySellPrice = 14
    val utilityBuyPrice = 14
    val energyCurrency = GBP
}

@CordaSerializable
enum class EnergyStateStatus { CREATED, TRADED }

/**
 * This is the main fact that lives on the ledger.
 * This state will be published every 30 minutes by the smart meter, which is tamper proof.
 *
 * The actual energy is: readingEnd - readingStart
 *
 */
data class EnergyEventState(
//        override val linearId: UniqueIdentifier,
        val household: Party,
        val interval: Int,  // 1.1.2019: 00:00-29 = 1, 00:30-01:00 = 2
        val readingStart: Int, // in kwh
        val readingEnd: Int, // in kwh
        val tradedStatus: EnergyStateStatus,
        var payed: Amount<Issued<Currency>>? = null,
        var received: Amount<Issued<Currency>>? = null
) : QueryableState {
//        , LinearState {

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return EnergySchemaV1.PersistentEnergyState()
    }

    override fun supportedSchemas() = listOf(EnergySchemaV1)

    override val participants: List<AbstractParty>
        get() = listOf(household)
}

class EnergyEventContract : Contract {
    override fun verify(tx: LedgerTransaction) {
        // TODO
    }
}

// Used to indicate the transaction's intent.
interface Commands : CommandData {
    class Consume : Commands
    class Trade : Commands
}

//class ReadingReconciliationState(){} //stuff
object EnergySchema

object EnergySchemaV1 : MappedSchema(schemaFamily = EnergySchema.javaClass, version = 1, mappedTypes = listOf(PersistentEnergyState::class.java)) {
    @Entity
    @Table(name = "energy_states")
    class PersistentEnergyState(
//TODO
    ) : PersistentState()

}
