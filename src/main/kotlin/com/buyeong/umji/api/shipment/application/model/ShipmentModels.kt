package com.buyeong.umji.api.shipment.application.model

import java.util.UUID

data class ShipmentRecord(
    val orderId: UUID,
    val status: String,
    val carrierCode: String?,
    val trackingNumber: String?,
    val reservationKeys: List<UUID>,
)

data class ShipmentChange(
    val orderId: UUID,
    val status: String,
    val carrierCode: String?,
    val trackingNumber: String?,
    val changed: Boolean,
)