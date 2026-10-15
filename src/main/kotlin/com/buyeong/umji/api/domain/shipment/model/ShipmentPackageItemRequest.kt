package com.buyeong.umji.api.domain.shipment.model

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.util.UUID

data class ShipmentPackageItemRequest(
    @field:NotNull
    val orderItemId: UUID,
    @field:Positive val boxQuantity: Int,
)
