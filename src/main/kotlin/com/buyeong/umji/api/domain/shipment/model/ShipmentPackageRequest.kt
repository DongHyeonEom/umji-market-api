package com.buyeong.umji.api.domain.shipment.model

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

data class ShipmentPackageRequest(
    @field:NotBlank @field:Size(max = 80)
    val carrierCode: String,
    @field:NotBlank @field:Size(max = 100)
    val trackingNumber: String,
    @field:PositiveOrZero
    val actualShippingCost: Long? = null,
    @field:NotEmpty
    @field:Valid
    val items: List<ShipmentPackageItemRequest>,
)
