package com.buyeong.umji.api.account.model

import com.buyeong.umji.api.account.application.model.BuyerGroupInvitation
import com.buyeong.umji.api.account.application.model.BuyerGroupJoinRequest
import com.buyeong.umji.api.account.application.model.BuyerGroupSearchResult
import com.buyeong.umji.api.account.application.model.BuyerGroupSummary
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class CreateIndividualGroupRequest(@field:NotBlank val name: String)
data class SearchBuyerGroupResponse(val id: UUID, val type: String, val name: String)
data class BuyerGroupResponse(val id: UUID, val type: String, val name: String, val representative: Boolean)
data class BuyerGroupInvitationResponse(val id: UUID, val groupId: UUID, val groupName: String, val invitedPhone: String)
data class BuyerGroupJoinRequestResponse(
    val id: UUID,
    val groupId: UUID,
    val groupName: String,
    val requesterName: String,
    val requesterPhone: String,
    val requestedAt: Instant,
)
data class InviteBuyerGroupMemberRequest(@field:NotBlank val phone: String)
data class RequestBuyerGroupJoinRequest(@field:NotNull val groupId: UUID)
data class DecideBuyerGroupJoinRequest(@field:NotNull val approved: Boolean)
data class SetBuyerGroupRepresentativeRequest(@field:NotNull val accountId: UUID)
data class BuyerGroupOnboardingResponse(val currentGroup: BuyerGroupResponse?, val invitations: List<BuyerGroupInvitationResponse>)

fun BuyerGroupSummary.toResponse() = BuyerGroupResponse(id, type, name, representative)
fun BuyerGroupSearchResult.toResponse() = SearchBuyerGroupResponse(id, type, name)
fun BuyerGroupInvitation.toResponse() = BuyerGroupInvitationResponse(id, groupId, groupName, invitedPhone)
fun BuyerGroupJoinRequest.toResponse() = BuyerGroupJoinRequestResponse(id, groupId, groupName, requesterName, requesterPhone, requestedAt)