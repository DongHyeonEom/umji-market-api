package com.buyeong.umji.api.domain.order.dto

import java.util.UUID

data class AdminPhoneOrderBuyerDto(
    val accountId: UUID,
    val accountName: String,
    val phone: String,
    val organizationId: UUID,
    val organizationName: String,
)