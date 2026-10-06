package com.buyeong.umji.api.account.model

import java.time.Instant
import java.util.UUID

data class OrganizationSummary(
    val id: UUID,
    val type: String,
    val name: String,
    val representative: Boolean,
    val capabilities: Set<String> = setOf("BUYER"),
)

data class OrganizationSearchResult(val id: UUID, val type: String, val name: String, val capabilities: Set<String> = setOf("BUYER"))

data class OrganizationInvitation(val id: UUID, val organizationId: UUID, val organizationName: String, val invitedPhone: String)

data class OrganizationJoinRequest(
    val id: UUID,
    val organizationId: UUID,
    val organizationName: String,
    val requesterName: String,
    val requesterPhone: String,
    val requestedAt: Instant,
)
