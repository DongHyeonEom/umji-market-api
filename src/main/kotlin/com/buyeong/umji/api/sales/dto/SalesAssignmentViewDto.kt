package com.buyeong.umji.api.sales.dto

import java.time.Instant
import java.util.UUID

data class SalesAssignmentViewDto(
    val id: UUID,
    val salesAccountId: UUID,
    val salesAccountName: String,
    val commissionRateBps: Int?,
    val assignmentReason: String,
    val validFrom: Instant,
    val validUntil: Instant?,
    val assignedByAccountId: UUID,
)