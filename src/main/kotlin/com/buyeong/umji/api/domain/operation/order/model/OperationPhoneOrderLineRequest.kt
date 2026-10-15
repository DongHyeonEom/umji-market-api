package com.buyeong.umji.api.domain.operation.order.model

import java.util.UUID
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class OperationPhoneOrderLineRequest(
    val salesOfferId: UUID,
    @field:Min(1)
    @field:Max(9999)
    val quantity: Int,
)
