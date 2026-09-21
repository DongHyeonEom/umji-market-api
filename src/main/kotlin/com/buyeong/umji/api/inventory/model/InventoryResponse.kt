package com.buyeong.umji.api.inventory.model

import java.time.Instant
import java.util.UUID

data class InventoryStockResponse(
    val skuId: UUID,
    val skuCode: String,
    val onHandQuantity: Int,
    val reservedQuantity: Int,
    val availableQuantity: Int,
    val safetyStockQuantity: Int,
)

data class InventoryMovementResponse(
    val id: Long,
    val skuId: UUID,
    val skuCode: String,
    val movementType: String,
    val quantityDelta: Int,
    val referenceType: String?,
    val referenceId: UUID?,
    val memo: String?,
    val occurredAt: Instant,
)

data class InventoryMovementPageResponse(
    val items: List<InventoryMovementResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
