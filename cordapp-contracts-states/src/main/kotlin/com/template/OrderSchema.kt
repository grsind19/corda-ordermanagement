package com.template.schema


import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object OrderSchema
object OrderSchemaV1:MappedSchema(
        schemaFamily = OrderSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistantOrder::class.java)
)
{
    @Entity
    @Table( name = "orderstate")
    class PersistantOrder(
            @Column(name="orderId")
            var orderId: String,

            @Column(name="prodName")
            var prodName: String,

            @Column(name="qty")
            var qty: Int,

            @Column(name="price")
            var price: Int,

            @Column(name="orderDate")
            var orderDate: String,

            @Column(name="manufacturer")
            var manufacturer: String,

            @Column(name="vendor")
            var vendor: String,

            @Column(name="id")
            var id: UUID

    ):PersistentState(){
        constructor():this("","",0,0,"","","", UUID.randomUUID())
    }

}