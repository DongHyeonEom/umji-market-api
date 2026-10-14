package com.buyeong.umji.api.domain.shipment.dto

import java.time.Instant
import java.util.UUID

data class ShipmentSettlementPackageDto(
    val packageId: UUID,
    val orderId: UUID,
    val orderNumber: String,
    val orderedAt: Instant,
    val registeredAt: Instant,
    val shippingStatus: String,
    val carrierCode: String,
    val trackingNumber: String,
    val actualShippingCost: Long?,
    val items: List<ShipmentSettlementItemDto>,
)