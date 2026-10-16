package com.buyeong.umji.api.domain.operation.account.dto

import java.util.UUID

data class OrganizationCommercePolicyDto(
    val organizationId: UUID,
    val buyerType: String,
    val paymentBeforeShipmentRequired: Boolean,
)
