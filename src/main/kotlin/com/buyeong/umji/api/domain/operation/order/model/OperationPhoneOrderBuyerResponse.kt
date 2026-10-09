package com.buyeong.umji.api.domain.operation.order.model

import java.util.UUID

data class OperationPhoneOrderBuyerResponse(
    val accountId: UUID,
    val accountName: String,
    val phone: String,
    val organizationId: UUID,
    val organizationName: String,
)