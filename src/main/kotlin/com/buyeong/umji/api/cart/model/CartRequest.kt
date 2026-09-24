package com.buyeong.umji.api.cart.model

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class AddCartItemRequest(
    @field:NotNull val skuId: UUID,
    @field:Min(1) val quantity: Int,
)

data class UpdateCartItemRequest(
    @field:Min(1) val quantity: Int,
)