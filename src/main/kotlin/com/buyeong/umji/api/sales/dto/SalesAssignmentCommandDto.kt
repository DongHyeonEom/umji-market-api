package com.buyeong.umji.api.sales.dto

import java.util.UUID

data class SalesAssignmentCommandDto(
    val salesAccountId: UUID,
    val commissionRateBps: Int?,
    val assignmentReason: String,
    val assignedByAccountId: UUID,
)