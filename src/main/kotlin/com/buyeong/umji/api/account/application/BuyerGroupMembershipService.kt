package com.buyeong.umji.api.account.application

import com.buyeong.umji.api.account.application.model.BuyerGroupInvitation
import com.buyeong.umji.api.account.application.model.BuyerGroupJoinRequest
import com.buyeong.umji.api.account.application.model.BuyerGroupSearchResult
import com.buyeong.umji.api.account.application.model.BuyerGroupSummary
import com.buyeong.umji.api.account.application.port.`in`.BuyerGroupMembershipUseCase
import com.buyeong.umji.api.account.application.port.out.BuyerGroupMembershipPort
import com.buyeong.umji.api.util.PhoneNumberHelper
import java.util.UUID

class BuyerGroupMembershipService(private val groups: BuyerGroupMembershipPort) : BuyerGroupMembershipUseCase {
    override fun current(accountId: UUID): BuyerGroupSummary? = groups.current(accountId)

    override fun createIndividualGroup(accountId: UUID, name: String): BuyerGroupSummary {
        require(name.isNotBlank() && name.length <= 200) { "그룹 이름은 1자 이상 200자 이하여야 합니다." }
        return groups.createIndividualGroup(accountId, name.trim())
    }

    override fun search(phone: String): List<BuyerGroupSearchResult> =
        groups.search(PhoneNumberHelper.normalizeMobilePhoneNumber(phone))

    override fun invite(accountId: UUID, phone: String): BuyerGroupInvitation =
        groups.invite(accountId, PhoneNumberHelper.normalizeMobilePhoneNumber(phone))

    override fun invitations(accountId: UUID): List<BuyerGroupInvitation> = groups.invitations(accountId)

    override fun respondInvitation(accountId: UUID, invitationId: UUID, accept: Boolean) =
        groups.respondInvitation(accountId, invitationId, accept)

    override fun requestToJoin(accountId: UUID, groupId: UUID) = groups.requestToJoin(accountId, groupId)

    override fun pendingJoinRequests(accountId: UUID): List<BuyerGroupJoinRequest> = groups.pendingJoinRequests(accountId)

    override fun respondJoinRequest(accountId: UUID, requestId: UUID, approve: Boolean) =
        groups.respondJoinRequest(accountId, requestId, approve)
}