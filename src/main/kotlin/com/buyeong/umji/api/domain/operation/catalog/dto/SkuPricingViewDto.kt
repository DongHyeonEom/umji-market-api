package com.buyeong.umji.api.domain.operation.catalog.dto

import java.util.UUID

data class SkuPricingViewDto(
    val skuId: UUID,
    val productId: UUID,
    val retailSalePrice: Long,
    val wholesaleSalePrice: Long,
    val wholesaleMinimumQuantity: Int,
    val salesStatus: String,
    val unitsPerSale: Int,
    val salesOfferId: UUID,
    val organizationId: UUID?,
)
