package com.buyeong.umji.api.domain.sales.dto

data class SalesAssignmentSnapshotDto(
    val organizationId: Long,
    val assignmentId: Long?,
    val salesAccountId: Long?,
    val commissionRateBps: Int?,
)