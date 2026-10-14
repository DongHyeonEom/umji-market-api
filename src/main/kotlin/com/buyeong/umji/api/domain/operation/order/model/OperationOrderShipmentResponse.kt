package com.buyeong.umji.api.domain.operation.order.model

import java.util.UUID

data class OperationOrderShipmentResponse(
    val packageId: UUID,
    val brandId: UUID?,
    val status: String,
    val carrierCode: String,
    val trackingNumber: String,
    val actualShippingCost: Long?,
)