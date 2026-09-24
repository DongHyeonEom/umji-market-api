package com.buyeong.umji.api.inventory.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class AdjustInventoryRequest(
    @field:NotNull val quantityDelta: Int,
    @field:NotBlank @field:Size(max = 30) val reason: String,
    @field:Size(max = 500) val memo: String? = null,
    val safetyStockQuantity: Int? = null,
)