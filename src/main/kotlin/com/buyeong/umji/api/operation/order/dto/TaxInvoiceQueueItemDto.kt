package com.buyeong.umji.api.operation.order.dto

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class TaxInvoiceQueueItemDto(
    val orderId: UUID,
    val orderNumber: String,
    val orderStatus: String,
    val buyerName: String,
    val buyerPhoneSuffix: String?,
    val orderedAt: Instant,
    val orderAmount: Long,
    val supplierBusinessName: String?,
    val buyerBusinessName: String?,
    val items: List<TaxInvoiceOrderItemDto>,
    val status: String,
    val approvalNumber: String?,
    val issuedAt: LocalDate?,
    val writtenDate: LocalDate?,
    val supplyDate: LocalDate?,
    val supplyAmount: Long?,
    val taxAmount: Long?,
    val totalAmount: Long?,
    val issuedByAccountId: UUID?,
)