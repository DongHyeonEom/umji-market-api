package com.buyeong.umji.api.operation.order.model

import com.buyeong.umji.api.operation.order.dto.TaxInvoiceQueueDataDto

data class OperationTaxInvoicePageResponse(
    val items: List<OperationTaxInvoiceResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(data: TaxInvoiceQueueDataDto) = OperationTaxInvoicePageResponse(
            data.items.map(OperationTaxInvoiceResponse::from),
            data.page,
            data.size,
            data.totalElements,
            data.totalPages,
        )
    }
}