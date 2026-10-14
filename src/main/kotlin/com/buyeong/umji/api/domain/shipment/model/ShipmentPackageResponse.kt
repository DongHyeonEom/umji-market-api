package com.buyeong.umji.api.domain.shipment.model

import com.buyeong.umji.api.domain.shipment.dto.ShipmentPackageDto
import com.buyeong.umji.api.domain.shipment.dto.ShipmentPackageItemDto
import java.util.UUID

data class ShipmentPackageItemResponse(
    val orderItemId: UUID,
    val skuId: UUID,
    val skuName: String,
    val boxQuantity: Int,
    val amountPerBox: Long?,
    val lineShippingCost: Long?,
)

data class ShipmentPackageResponse(
    val orderId: UUID,
    val packageId: UUID,
    val brandId: UUID?,
    val shippingStatus: String,
    val carrierCode: String,
    val trackingNumber: String,
    val actualShippingCost: Long?,
    val items: List<ShipmentPackageItemResponse>,
    val trackingUrl: String? = null,
)

fun ShipmentPackageDto.toResponse() = ShipmentPackageResponse(
    orderId, packageId, brandId, status, carrierCode, trackingNumber, actualShippingCost,
    items.map(ShipmentPackageItemDto::toResponse),
)

private fun ShipmentPackageItemDto.toResponse() = ShipmentPackageItemResponse(
    orderItemId, skuId, skuName, boxQuantity, amountPerBox, lineShippingCost,
)
