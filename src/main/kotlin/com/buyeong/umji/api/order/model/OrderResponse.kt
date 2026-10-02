package com.buyeong.umji.api.order.model

import java.time.Instant
import java.util.UUID

data class OrderResponse(
    val id: UUID,
    val orderNumber: String,
    val status: String,
    val subtotalAmount: Long,
    val totalAmount: Long,
    val orderedAt: Instant,
    val items: List<OrderItemResponse>,
    val paymentMethod: String,
    val paymentStatus: String,
    val taxInvoiceRequested: Boolean,
    val depositBankName: String?,
    val depositAccountNumber: String?,
    val depositAccountHolder: String?,
    val shippingStatus: String,
    val carrierCode: String?,
    val trackingNumber: String?,
    val trackingUrl: String?,
    val cancellationRequestStatus: String?,
    val orderedByName: String?,
    val orderedByPhoneSuffix: String?,
    val shippingRecipientName: String?,
    val shippingRecipientPhone: String?,
    val shippingPostalCode: String?,
    val shippingAddress1: String?,
    val shippingAddress2: String?,
)

data class BankAccountInstructionsResponse(
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
)

data class OrderCheckoutOptionsResponse(
    val defaultTaxInvoiceRequested: Boolean,
    val standardBankAccount: BankAccountInstructionsResponse,
    val taxInvoiceBankAccount: BankAccountInstructionsResponse,
)

data class CreateOrderRequest(
    val shippingAddressId: UUID,
    val taxInvoiceRequested: Boolean? = null,
    val updateDefaultTaxInvoicePreference: Boolean = false,
)

data class OrderItemResponse(
    val id: UUID,
    val skuId: UUID,
    val productName: String,
    val skuName: String,
    val skuCode: String,
    val unitPrice: Long,
    val quantity: Int,
    val lineAmount: Long,
    val status: String,
)

data class OrderPageResponse(
    val items: List<OrderResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)