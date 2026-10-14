package com.buyeong.umji.api.domain.shipment.model

import com.buyeong.umji.api.domain.shipment.dto.BrandShippingManagerDto
import java.util.UUID

data class BrandShippingManagerResponse(val brandId: UUID, val accountId: UUID, val accountName: String)

fun BrandShippingManagerDto.toResponse() = BrandShippingManagerResponse(brandId, accountId, accountName)
