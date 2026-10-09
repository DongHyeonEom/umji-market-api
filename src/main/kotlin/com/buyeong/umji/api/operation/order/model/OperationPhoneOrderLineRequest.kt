package com.buyeong.umji.api.operation.order.model

import java.util.UUID

data class OperationPhoneOrderLineRequest(
    val salesOfferId: UUID,
    val quantity: Int,
)