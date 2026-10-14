package com.buyeong.umji.api.domain.shipment.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class UpdateShipmentPackageCostRequest(
    @field:PositiveOrZero
    val actualShippingCost: Long,

    @field:NotBlank
    @field:Size(max = 500,)
    val reason: String,
)