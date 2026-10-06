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
    val salesOfferId: UUID = UUID(0, 0),
    val channelCode: String = "WHOLESALE",
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
    val shippingAddress: ShippingAddressSnapshot,
    val items: List<OrderItemDraft>,
    val taxInvoiceSnapshot: TaxInvoiceSnapshotDraft? = null,
    val channelCode: String = "WHOLESALE",
)

data class ShippingAddressSnapshot(
    val recipientName: String,
    val recipientPhone: String,
    val postalCode: String,
    val address1: String,
    val address2: String?,
)

data class BankAccountInstructions(
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
)

data class OrderCheckoutOptions(
    val defaultTaxInvoiceRequested: Boolean,
    val taxInvoiceAvailable: Boolean,
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
    val salesOfferId: UUID = UUID(0, 0),
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
    val shippingRecipientName: String? = null,
    val shippingRecipientPhone: String? = null,
    val shippingPostalCode: String? = null,
    val shippingAddress1: String? = null,
    val shippingAddress2: String? = null,
    val taxInvoiceSnapshot: TaxInvoiceSnapshot? = null,
    val channelCode: String = "WHOLESALE",
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
    val salesOfferId: UUID = UUID(0, 0),
)

data class OrderPage(
    val items: List<OrderView>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
