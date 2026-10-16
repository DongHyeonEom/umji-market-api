package com.buyeong.umji.api.domain.operation.catalog.dto

import java.util.UUID

data class SkuPricingCommandDto(
    val skuId: UUID,
    val retailSalePrice: Long,
    val wholesaleSalePrice: Long,
    val wholesaleMinimumQuantity: Int,
    val salesStatus: String,
    val unitsPerSale: Int,
)
