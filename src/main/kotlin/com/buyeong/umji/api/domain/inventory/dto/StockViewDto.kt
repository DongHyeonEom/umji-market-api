package com.buyeong.umji.api.domain.inventory.dto

import java.util.UUID

data class StockViewDto(
    val skuId: UUID,
    val skuCode: String,
    val onHand: Int,
    val reserved: Int,
    val available: Int,
    val safety: Int,
    val unitsPerSale: Int = 1,
) {
    val onHandBoxes: Int get() = onHand / unitsPerSale
    val onHandRemainder: Int get() = onHand % unitsPerSale
    val reservedBoxes: Int get() = reserved / unitsPerSale
    val reservedRemainder: Int get() = reserved % unitsPerSale
    val availableBoxes: Int get() = available / unitsPerSale
    val availableRemainder: Int get() = available % unitsPerSale
}