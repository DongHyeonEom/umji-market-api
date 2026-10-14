package com.buyeong.umji.api.domain.operation.order.model

data class OperationOrderPageResponse(
    val items: List<OperationOrderSearchItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)