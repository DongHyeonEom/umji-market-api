package com.buyeong.umji.api.domain.shipment.model

import com.buyeong.umji.api.domain.shipment.dto.BrandShippingRateDto
import java.util.UUID

data class BrandShippingRateResponse(
    val brandId: UUID,
    val skuId: UUID,
    val skuName: String,
    val carrierCode: String,
    val amountPerBox: Long,
)

fun BrandShippingRateDto.toResponse() = BrandShippingRateResponse(brandId, skuId, skuName, carrierCode, amountPerBox)
