package com.buyeong.umji.api.order.model

import java.time.Instant
import java.util.UUID

data class CancellationOrder(
    val id: UUID,
    val accountId: UUID,
    val orderNumber: String,
    val orderStatus: String,
    val paymentStatus: String,
    val shippingStatus: String,
    val reservationKeys: List<UUID>,
)

data class CancellationChange(val orderId: UUID, val orderStatus: String, val requestStatus: String)
data class CancellationQueueItem(val orderId: UUID, val orderNumber: String, val accountId: UUID, val requestedAt: Instant)
data class CancellationQueuePage(val items: List<CancellationQueueItem>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)
