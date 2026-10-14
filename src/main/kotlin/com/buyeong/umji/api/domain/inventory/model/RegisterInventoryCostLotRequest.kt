package com.buyeong.umji.api.domain.inventory.model

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class RegisterInventoryCostLotRequest(
    @field:NotNull val organizationId: UUID,
    @field:NotBlank @field:Size(max = 120) val containerReference: String,
    @field:Min(1) val quantity: Int,
    @field:Min(0) val totalPurchaseCost: Long,
)
