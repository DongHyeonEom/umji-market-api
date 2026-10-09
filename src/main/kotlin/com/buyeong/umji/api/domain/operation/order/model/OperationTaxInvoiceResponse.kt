package com.buyeong.umji.api.domain.operation.order.model

import com.buyeong.umji.api.domain.operation.order.dto.TaxInvoiceOrderItemDto
import com.buyeong.umji.api.domain.operation.order.dto.TaxInvoiceQueueItemDto
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class OperationTaxInvoiceResponse(
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
) {
    companion object {
        fun from(item: TaxInvoiceQueueItemDto) = OperationTaxInvoiceResponse(
            item.orderId, item.orderNumber, item.orderStatus, item.buyerName, item.buyerPhoneSuffix,
            item.orderedAt, item.orderAmount, item.supplierBusinessName, item.buyerBusinessName, item.items,
            item.status, item.approvalNumber, item.issuedAt,
            item.writtenDate, item.supplyDate, item.supplyAmount, item.taxAmount, item.totalAmount,
            item.issuedByAccountId,
        )
    }
}