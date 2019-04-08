package com.template.state

import com.template.schema.OrderSchemaV1
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

data class OrderState(val orderID:String,
                      val prodName:String,
                      val qty:Int,
                      val price:Int,
                      val orderDate:String,
                      val manufacturer: Party,
                      val vendor:Party,
                      override val linearId: UniqueIdentifier= UniqueIdentifier()):LinearState,QueryableState{
    override val participants: List<AbstractParty>
        get() = listOf(manufacturer,vendor)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when(schema)
        {
            is OrderSchemaV1 -> OrderSchemaV1.PersistantOrder(
                    this.orderID,
                    this.prodName,
                    this.qty,
                    this.price,
                    this.orderDate,
                    this.manufacturer.toString(),
                    this.vendor.toString(),
                    this.linearId.id
            )
            else-> throw IllegalArgumentException("There is no schemas found")
        }


    }

    override fun supportedSchemas(): Iterable<MappedSchema> {
        return listOf(OrderSchemaV1)

    }
}

