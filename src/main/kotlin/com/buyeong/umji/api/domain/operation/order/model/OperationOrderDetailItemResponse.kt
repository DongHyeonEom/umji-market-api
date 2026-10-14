package com.buyeong.umji.api.domain.operation.order.model

data class OperationOrderDetailItemResponse(
    val productName: String,
    val skuName: String,
    val skuCode: String,
    val unitPrice: Long,
    val quantity: Int,
    val lineAmount: Long,
)