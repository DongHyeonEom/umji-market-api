package com.buyeong.umji.api.domain.inventory.dto

data class StockStateDto(val sku: SkuReferenceDto, val onHand: Int, val reserved: Int, val safety: Int) {
    val available: Int get() = onHand - reserved
}