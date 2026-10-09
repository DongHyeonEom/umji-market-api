package com.buyeong.umji.api.order.dto

import java.time.Instant
import java.util.UUID

data class CancellationQueueItemDto(val orderId: UUID, val orderNumber: String, val accountId: UUID, val requestedAt: Instant)