package com.buyeong.umji.api.domain.inventory.model

import java.util.UUID

data class InventoryCostLotResponse(
    val lotId: Long,
    val skuId: UUID,
    val organizationId: UUID?,
    val containerReference: String,
    val receivedQuantity: Int,
    val remainingQuantity: Int,
    val reservedQuantity: Int,
    val totalPurchaseCost: Long?,
    val unitCost: Long?,
)
