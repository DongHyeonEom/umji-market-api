package com.buyeong.umji.api.inventory.dto

import java.time.Instant
import java.util.UUID

data class ReservationStateDto(val key: UUID, val sku: SkuReferenceDto, val quantity: Int, val status: String, val expiresAt: Instant?, val releasedAt: Instant? = null)