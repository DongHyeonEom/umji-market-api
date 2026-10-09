package com.buyeong.umji.api.domain.shipment.dto

import java.util.UUID

data class ShipmentChangeDto(
    val orderId: UUID,
    val status: String,
    val carrierCode: String?,
    val trackingNumber: String?,
    val changed: Boolean,
)