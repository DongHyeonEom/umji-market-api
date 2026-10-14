package com.buyeong.umji.api.domain.shipment.dto

import java.util.UUID

data class ShipmentPackageItemDto(
    val orderItemId: UUID,
    val skuId: UUID,
    val skuName: String,
    val boxQuantity: Int,
    val amountPerBox: Long?,
    val lineShippingCost: Long?,
)
