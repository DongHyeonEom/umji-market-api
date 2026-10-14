package com.buyeong.umji.api.domain.shipment.dto

import java.util.UUID

data class ShipmentPackageDto(
    val orderId: UUID,
    val packageId: UUID,
    val brandId: UUID?,
    val status: String,
    val carrierCode: String,
    val trackingNumber: String,
    val actualShippingCost: Long?,
    val items: List<ShipmentPackageItemDto>,
)
