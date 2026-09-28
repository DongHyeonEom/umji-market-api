package com.buyeong.umji.api.shipment.model

import com.buyeong.umji.api.shipment.application.model.ShipmentChange
import java.util.UUID

data class ShipmentResponse(
    val orderId: UUID,
    val shippingStatus: String,
    val carrierCode: String?,
    val trackingNumber: String?,
)

fun ShipmentChange.toResponse() = ShipmentResponse(orderId, status, carrierCode, trackingNumber)