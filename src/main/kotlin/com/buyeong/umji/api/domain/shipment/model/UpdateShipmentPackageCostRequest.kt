package com.buyeong.umji.api.domain.shipment.model

import jakarta.validation.constraints.PositiveOrZero

data class UpdateShipmentPackageCostRequest(
    @field:PositiveOrZero
    val actualShippingCost: Long,
)
