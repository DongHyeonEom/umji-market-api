package com.buyeong.umji.api.payment.model

import java.time.Instant
import java.util.UUID

data class PaymentQueueItem(
    val orderId: UUID,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String?,
    val orderAmount: Long,
    val paymentMethod: String,
    val paymentStatus: String,
    val updatedAt: Instant,
)

data class PaymentQueuePage(
    val items: List<PaymentQueueItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class PaymentRecord(
    val orderId: UUID,
    val orderStatus: String,
    val paymentStatus: String,
)

data class PaymentStatusChange(
    val orderId: UUID,
    val orderStatus: String,
    val paymentStatus: String,
    val changed: Boolean,
)
