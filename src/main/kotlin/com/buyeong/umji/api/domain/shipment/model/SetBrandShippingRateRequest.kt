package com.buyeong.umji.api.domain.shipment.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import java.util.UUID

data class SetBrandShippingRateRequest(
    val skuId: UUID,
    @field:NotBlank
    val carrierCode: String,
    @field:PositiveOrZero
    val amountPerBox: Long,
)
