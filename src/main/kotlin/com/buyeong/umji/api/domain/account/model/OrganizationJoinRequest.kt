package com.buyeong.umji.api.domain.account.model

import java.time.Instant
import java.util.UUID

data class OrganizationJoinRequest(
    val id: UUID,
    val organizationId: UUID,
    val organizationName: String,
    val requesterName: String,
    val requesterPhone: String,
    val requestedAt: Instant,
)