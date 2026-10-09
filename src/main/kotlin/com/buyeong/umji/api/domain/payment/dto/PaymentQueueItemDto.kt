package com.buyeong.umji.api.domain.payment.dto

import java.time.Instant
import java.util.UUID

data class PaymentQueueItemDto(
    val orderId: UUID,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String?,
    val orderAmount: Long,
    val paymentMethod: String,
    val paymentStatus: String,
    val updatedAt: Instant,
)