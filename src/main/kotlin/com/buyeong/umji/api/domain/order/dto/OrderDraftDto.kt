package com.buyeong.umji.api.domain.order.dto

import java.time.Instant
import java.util.UUID

data class OrderDraftDto(
    val accountId: UUID,
    val status: String,
    val orderedAt: Instant,
    val subtotalAmount: Long,
    val totalAmount: Long,
    val taxInvoiceRequested: Boolean,
    val depositBankName: String,
    val depositAccountNumber: String,
    val depositAccountHolder: String,
    val shippingAddress: ShippingAddressSnapshotDto,
    val items: List<OrderItemDraftDto>,
    val taxInvoiceSnapshot: TaxInvoiceSnapshotDraftDto? = null,
    val channelCode: String = "WHOLESALE",
    val createdByAccountId: UUID = accountId,
    val orderSource: String = "CUSTOMER",
)