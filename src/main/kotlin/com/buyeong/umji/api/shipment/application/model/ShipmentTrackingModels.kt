package com.buyeong.umji.api.shipment.application.model

import java.util.UUID

data class ShipmentTrackingCandidate(
    val orderId: UUID,
    val status: String,
    val carrierCode: String,
    val trackingNumber: String,
)

enum class CarrierTrackingStatus {
    IN_TRANSIT,
    DELIVERED,
    NOT_FOUND,
    UNAVAILABLE,
}