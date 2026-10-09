package com.buyeong.umji.api.payment.dto

data class PaymentQueuePageDto(
    val items: List<PaymentQueueItemDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)