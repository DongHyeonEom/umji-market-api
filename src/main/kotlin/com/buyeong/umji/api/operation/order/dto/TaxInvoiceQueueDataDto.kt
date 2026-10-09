package com.buyeong.umji.api.operation.order.dto

data class TaxInvoiceQueueDataDto(val items: List<TaxInvoiceQueueItemDto>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)