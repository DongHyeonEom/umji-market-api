package com.buyeong.umji.api.domain.shipment.model

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.PositiveOrZero

data class ShipmentPackageRequest(
    @field:NotBlank
    val carrierCode: String,
    @field:NotBlank
    val trackingNumber: String,
    @field:PositiveOrZero
    val actualShippingCost: Long? = null,
    @field:NotEmpty
    @field:Valid
    val items: List<ShipmentPackageItemRequest>,
)
