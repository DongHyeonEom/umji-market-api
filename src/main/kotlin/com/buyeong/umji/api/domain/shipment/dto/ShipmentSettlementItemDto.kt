package com.buyeong.umji.api.domain.shipment.dto

import java.util.UUID

data class ShipmentSettlementItemDto(
    val orderItemId: UUID,
    val skuName: String,
    val boxQuantity: Int,
    val amountPerBox: Long?,
    val lineShippingCost: Long?,
)