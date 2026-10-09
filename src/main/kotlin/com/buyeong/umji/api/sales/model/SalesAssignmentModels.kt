package com.buyeong.umji.api.sales.model

import java.time.Instant
import java.util.UUID

data class SalesAssignmentCommand(
    val salesAccountId: UUID,
    val commissionRateBps: Int?,
    val assignmentReason: String,
    val assignedByAccountId: UUID,
)

data class SalesAssignmentView(
    val id: UUID,
    val salesAccountId: UUID,
    val salesAccountName: String,
    val commissionRateBps: Int?,
    val assignmentReason: String,
    val validFrom: Instant,
    val validUntil: Instant?,
    val assignedByAccountId: UUID,
)
