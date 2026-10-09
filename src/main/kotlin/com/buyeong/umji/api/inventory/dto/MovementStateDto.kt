package com.buyeong.umji.api.inventory.dto

import java.time.Instant
import java.util.UUID

data class MovementStateDto(
    val id: Long,
    val sku: SkuReferenceDto,
    val type: String,
    val delta: Int,
    val referenceType: String?,
    val referenceId: UUID?,
    val memo: String?,
    val occurredAt: Instant,
)