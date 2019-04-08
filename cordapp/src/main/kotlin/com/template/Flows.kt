package com.template.flow


import co.paralleluniverse.fibers.Suspendable
import com.template.contract.OrderContract
import com.template.contract.OrderContract.Companion.CONTRACT_ID
import com.template.state.OrderState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import com.template.flow.OrderCreateFlow.Initiator
import com.template.flow.OrderCreateFlow.Acceptor
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object OrderCreateFlow {
    @InitiatingFlow //iniating
    @StartableByRPC//should wait for subflow
    class Initiator(val orderID:String,
                    val prodName:String,
                    val qty:Int,
                    val price:Int,
                    val orderDate:String,
                    val vendor: Party):FlowLogic<SignedTransaction>()
    {
        companion object {
            object GENERATE_TRANSACTION: ProgressTracker.Step("Generating transaction based on new Contract")
            object VERIFY_TRANSACTION: ProgressTracker.Step("Verifing transaction with the smart Contract")
            object SIGNING_TRANSACTION: ProgressTracker.Step("Signing the transaction in the iniator Node")
            object GATHERING_SIGNATURE: ProgressTracker.Step("Gathereing the counter party signature"){
                override fun childProgressTracker()= CollectSignaturesFlow.tracker()

            }

            object FINALYZING_TRANSACTION: ProgressTracker.Step("Obtaining notary signature and recording transaction"){
                override fun childProgressTracker()= FinalityFlow.tracker()

            }

            fun tracker()=ProgressTracker(
                    GENERATE_TRANSACTION,
                    VERIFY_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGNATURE,
                    FINALYZING_TRANSACTION
            )

        }
        override val progressTracker= tracker()

        @Suspendable
        override fun call(): SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.first()

            progressTracker.currentStep= GENERATE_TRANSACTION
            val order=OrderState(orderID,
                    prodName,
                    qty,
                    price,
                    orderDate,
                    serviceHub.myInfo.legalIdentities.first(),
                    vendor)
            val command= Command(OrderContract.Commands.Create(),order.participants.map { it.owningKey })
            val txBuilder=TransactionBuilder(notary)
                    .addOutputState(order,CONTRACT_ID)
                    .addCommand(command)

            progressTracker.currentStep= VERIFY_TRANSACTION
            txBuilder.verify(serviceHub)

            progressTracker.currentStep= SIGNING_TRANSACTION
            val partiallysignedtx=serviceHub.signInitialTransaction(txBuilder)

            progressTracker.currentStep= GATHERING_SIGNATURE
            val otherPartyFlow=initiateFlow(vendor)
            val fullysignedtx=subFlow(CollectSignaturesFlow(partiallysignedtx,setOf(otherPartyFlow), Companion.GATHERING_SIGNATURE.childProgressTracker()))

            progressTracker.currentStep= FINALYZING_TRANSACTION
            return subFlow(FinalityFlow(fullysignedtx, Companion.FINALYZING_TRANSACTION.childProgressTracker()))

        }
    }
    @InitiatedBy(OrderCreateFlow.Initiator::class)
    class Acceptor(val otherPartyFlow:FlowSession) :FlowLogic<SignedTransaction>()
    {
        @Suspendable
        override fun call(): SignedTransaction {
            val signedTransactionFlow=object :SignTransactionFlow(otherPartyFlow){
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val data=stx.tx.outputs.single().data
                    val order=data as OrderState
                    "The order quantity should be less than zero" using(order.qty>0)

                }
            }
            return subFlow(signedTransactionFlow)
        }
    }
}

