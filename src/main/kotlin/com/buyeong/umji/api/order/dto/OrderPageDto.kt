package com.buyeong.umji.api.order.dto

data class OrderPageDto(
    val items: List<OrderViewDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)