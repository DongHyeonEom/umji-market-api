package com.buyeong.umji.api.domain.operation.order.dto

import java.util.UUID

data class OperationOrderShipmentDto(
    val packageId: UUID,
    val brandId: UUID?,
    val status: String,
    val carrierCode: String,
    val trackingNumber: String,
    val actualShippingCost: Long?,
)