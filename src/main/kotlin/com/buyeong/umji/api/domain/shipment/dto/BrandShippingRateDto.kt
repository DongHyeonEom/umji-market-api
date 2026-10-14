package com.buyeong.umji.api.domain.shipment.dto

import java.util.UUID

data class BrandShippingRateDto(
    val brandId: UUID,
    val skuId: UUID,
    val skuName: String,
    val carrierCode: String,
    val amountPerBox: Long,
)
