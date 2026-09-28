package com.buyeong.umji.api.shipment.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ShipmentTrackingRequest(
    @field:NotBlank
    @field:Size(max = 80)
    val carrierCode: String,
    @field:NotBlank
    @field:Size(max = 100)
    val trackingNumber: String,
)