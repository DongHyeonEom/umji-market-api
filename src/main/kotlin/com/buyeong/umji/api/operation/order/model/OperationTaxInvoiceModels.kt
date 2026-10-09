package com.buyeong.umji.api.operation.order.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class OperationTaxInvoicePageResponse(
    val items: List<OperationTaxInvoiceResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(data: TaxInvoiceQueueData) = OperationTaxInvoicePageResponse(
            data.items.map(OperationTaxInvoiceResponse::from), data.page, data.size, data.totalElements, data.totalPages,
        )
    }
}

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
) {
    companion object {
        fun from(item: TaxInvoiceQueueItem) = OperationTaxInvoiceResponse(
            item.orderId, item.orderNumber, item.orderStatus, item.buyerName, item.buyerPhoneSuffix,
            item.orderedAt, item.orderAmount, item.supplierBusinessName, item.buyerBusinessName, item.items,
            item.status, item.approvalNumber, item.issuedAt,
            item.writtenDate, item.supplyDate, item.supplyAmount, item.taxAmount, item.totalAmount,
            item.issuedByAccountId,
        )
    }
}

data class OperationManualTaxInvoiceRequest(
    @field:NotBlank @field:Size(max = 100,) val approvalNumber: String,
    @field:NotNull val issuedAt: LocalDate,
    @field:NotNull val writtenDate: LocalDate,
    @field:NotNull val supplyDate: LocalDate,
    @field:PositiveOrZero val supplyAmount: Long,
    @field:PositiveOrZero val taxAmount: Long,
    @field:PositiveOrZero val totalAmount: Long,
    @field:Size(max = 500,) val reason: String? = null,
)
