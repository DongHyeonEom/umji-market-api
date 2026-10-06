package com.buyeong.umji.api.account.application.port.`in`

import com.buyeong.umji.api.account.application.model.BuyerGroupInvitation
import com.buyeong.umji.api.account.application.model.BuyerGroupJoinRequest
import com.buyeong.umji.api.account.application.model.BuyerGroupSearchResult
import com.buyeong.umji.api.account.application.model.BuyerGroupSummary
import com.buyeong.umji.api.account.application.model.BuyerGroupRegistrationCommand
import java.util.UUID

interface BuyerGroupMembershipUseCase {
    fun current(accountId: UUID): BuyerGroupSummary?
    fun createIndividualGroup(accountId: UUID, name: String): BuyerGroupSummary
    fun register(accountId: UUID, command: BuyerGroupRegistrationCommand): BuyerGroupSummary
    fun search(phone: String): List<BuyerGroupSearchResult>
    fun invite(accountId: UUID, phone: String): BuyerGroupInvitation
    fun invitations(accountId: UUID): List<BuyerGroupInvitation>
    fun respondInvitation(accountId: UUID, invitationId: UUID, accept: Boolean)
    fun requestToJoin(accountId: UUID, groupId: UUID)
    fun pendingJoinRequests(accountId: UUID): List<BuyerGroupJoinRequest>
    fun respondJoinRequest(accountId: UUID, requestId: UUID, approve: Boolean)
}
