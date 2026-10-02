package com.buyeong.umji.api.account.application.model

import java.time.Instant
import java.util.UUID

data class BuyerGroupSummary(
    val id: UUID,
    val type: String,
    val name: String,
    val representative: Boolean,
)

data class BuyerGroupSearchResult(val id: UUID, val type: String, val name: String)

data class BuyerGroupInvitation(val id: UUID, val groupId: UUID, val groupName: String, val invitedPhone: String)

data class BuyerGroupJoinRequest(
    val id: UUID,
    val groupId: UUID,
    val groupName: String,
    val requesterName: String,
    val requesterPhone: String,
    val requestedAt: Instant,
)