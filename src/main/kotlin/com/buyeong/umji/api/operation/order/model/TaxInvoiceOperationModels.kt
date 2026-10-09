package com.buyeong.umji.api.operation.order.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class TaxInvoiceQueueData(val items: List<TaxInvoiceQueueItem>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)

data class TaxInvoiceQueueItem(
    val orderId: UUID,
    val orderNumber: String,
    val orderStatus: String,
    val buyerName: String,
    val buyerPhoneSuffix: String?,
    val orderedAt: Instant,
    val orderAmount: Long,
    val supplierBusinessName: String?,
    val buyerBusinessName: String?,
    val items: List<TaxInvoiceOrderItem>,
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

data class TaxInvoiceOrderItem(
    val productName: String,
    val skuCode: String,
    val quantity: Int,
    val unitPrice: Long,
    val lineAmount: Long,
)
