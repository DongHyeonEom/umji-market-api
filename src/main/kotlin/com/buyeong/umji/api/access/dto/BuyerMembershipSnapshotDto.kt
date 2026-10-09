package com.buyeong.umji.api.access.dto

import java.util.UUID

data class BuyerMembershipSnapshotDto(
    val organizationId: UUID,
    val isRepresentative: Boolean,
)