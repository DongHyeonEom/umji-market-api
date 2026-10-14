package com.buyeong.umji.api.domain.shipment.model

import java.util.UUID

data class ShipmentSettlementItemResponse(
    val orderItemId: UUID,
    val skuName: String,
    val boxQuantity: Int,
    val amountPerBox: Long?,
    val lineShippingCost: Long?,
)