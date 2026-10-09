package com.buyeong.umji.api.domain.shipment.dto

import java.util.UUID

data class ShipmentRecordDto(
    val orderId: UUID,
    val status: String,
    val carrierCode: String?,
    val trackingNumber: String?,
    val reservationKeys: List<UUID>,
)