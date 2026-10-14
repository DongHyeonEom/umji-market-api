package com.buyeong.umji.api.domain.operation.order.dto

data class OperationOrderDetailItemDto(
    val productName: String,
    val skuName: String,
    val skuCode: String,
    val unitPrice: Long,
    val quantity: Int,
    val lineAmount: Long,
)