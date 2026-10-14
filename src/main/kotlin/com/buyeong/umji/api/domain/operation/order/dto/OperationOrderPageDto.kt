package com.buyeong.umji.api.domain.operation.order.dto

data class OperationOrderPageDto(
    val items: List<OperationOrderSearchRowDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)