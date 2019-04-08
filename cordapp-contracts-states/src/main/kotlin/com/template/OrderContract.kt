package com.template.contract

import com.template.state.OrderState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class OrderContract:Contract {
    companion object {
        @JvmStatic
        val CONTRACT_ID="com.template.contract.OrderContract"
    }

    override fun verify(tx: LedgerTransaction) {
        val Command=tx.commands.requireSingleCommand<Commands.Create>()

        requireThat {
            "No input state should be allowed" using(tx.inputs.isEmpty())
            val order=tx.outputsOfType<OrderState>().single()

            "Order quantity should not be empty" using(order.qty>0)
            "Manufacturer and vendor should not be same" using(order.manufacturer!=order.vendor)
            "All the participants must be signers" using(Command.signers.containsAll(order.participants.map{it.owningKey}))
        }

    }
    interface Commands:CommandData{
        class Create:Commands
        class Edit:Commands //for edit command
    }
}

