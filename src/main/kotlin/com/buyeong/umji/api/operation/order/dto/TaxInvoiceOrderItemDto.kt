package com.buyeong.umji.api.operation.order.dto

data class TaxInvoiceOrderItemDto(
    val productName: String,
    val skuCode: String,
    val quantity: Int,
    val unitPrice: Long,
    val lineAmount: Long,
)