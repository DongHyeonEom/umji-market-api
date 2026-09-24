package com.buyeong.umji.api.inventory.application.model

import java.time.Instant
import java.util.UUID

data class SkuReference(val id: UUID, val code: String)
data class StockState(val sku: SkuReference, val onHand: Int, val reserved: Int, val safety: Int) {
    val available: Int get() = onHand - reserved
}
data class ReservationState(val key: UUID, val sku: SkuReference, val quantity: Int, val status: String, val expiresAt: Instant?, val releasedAt: Instant? = null)
data class MovementState(
    val id: Long,
    val sku: SkuReference,
    val type: String,
    val delta: Int,
    val referenceType: String?,
    val referenceId: UUID?,
    val memo: String?,
    val occurredAt: Instant,
)
data class MovementPageState(val items: List<MovementState>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)
data class StockView(val skuId: UUID, val skuCode: String, val onHand: Int, val reserved: Int, val available: Int, val safety: Int)