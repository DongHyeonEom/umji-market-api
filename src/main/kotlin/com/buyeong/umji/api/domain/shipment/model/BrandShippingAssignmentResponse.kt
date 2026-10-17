package com.buyeong.umji.api.domain.shipment.model

import com.buyeong.umji.api.domain.shipment.dto.BrandShippingAssignmentDto
import java.util.UUID

data class BrandShippingAssignmentResponse(val brandId: UUID, val accountId: UUID, val accountName: String)

fun BrandShippingAssignmentDto.toResponse() = BrandShippingAssignmentResponse(brandId, accountId, accountName)
