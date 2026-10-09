package com.buyeong.umji.api.domain.order.dto

import java.time.Instant
import java.util.UUID

data class OrderViewDto(
    val id: UUID,
    val orderNumber: String,
    val status: String,
    val subtotalAmount: Long,
    val totalAmount: Long,
    val orderedAt: Instant,
    val items: List<OrderItemViewDto>,
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
    val taxInvoiceSnapshot: TaxInvoiceSnapshotDto? = null,
    val channelCode: String = "WHOLESALE",
    val sellerOrganizationId: UUID? = null,
)