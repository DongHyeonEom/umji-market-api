package com.buyeong.umji.api.order.application.model

import java.time.Instant
import java.util.UUID

data class CheckoutLine(
    val skuId: UUID,
    val skuCode: String,
    val productName: String,
    val skuName: String,
    val unitPrice: Long,
    val quantity: Int,
    val salesStatus: String,
)

data class OrderDraft(
    val accountId: UUID,
    val status: String,
    val orderedAt: Instant,
    val subtotalAmount: Long,
    val totalAmount: Long,
    val taxInvoiceRequested: Boolean,
    val depositBankName: String,
    val depositAccountNumber: String,
    val depositAccountHolder: String,
    val items: List<OrderItemDraft>,
)

data class BankAccountInstructions(
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
)

data class OrderCheckoutOptions(
    val defaultTaxInvoiceRequested: Boolean,
    val standardBankAccount: BankAccountInstructions,
    val taxInvoiceBankAccount: BankAccountInstructions,
)

data class OrderItemDraft(
    val skuId: UUID,
    val productName: String,
    val skuName: String,
    val skuCode: String,
    val unitPrice: Long,
    val quantity: Int,
    val lineAmount: Long,
    val reservationKey: UUID,
    val status: String,
)

data class OrderView(
    val id: UUID,
    val orderNumber: String,
    val status: String,
    val subtotalAmount: Long,
    val totalAmount: Long,
    val orderedAt: Instant,
    val items: List<OrderItemView>,
    val paymentMethod: String = "BANK_TRANSFER",
    val paymentStatus: String = "WAITING_FOR_DEPOSIT",
    val taxInvoiceRequested: Boolean = false,
    val depositBankName: String? = null,
    val depositAccountNumber: String? = null,
    val depositAccountHolder: String? = null,
    val shippingStatus: String = "READY_TO_SHIP",
    val carrierCode: String? = null,
    val trackingNumber: String? = null,
    val cancellationRequestStatus: String? = null,
    val orderedByName: String? = null,
    val orderedByPhoneSuffix: String? = null,
)

data class OrderItemView(
    val id: UUID,
    val skuId: UUID,
    val reservationKey: UUID,
    val productName: String,
    val skuName: String,
    val skuCode: String,
    val unitPrice: Long,
    val quantity: Int,
    val lineAmount: Long,
    val status: String,
)

data class OrderPage(
    val items: List<OrderView>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)