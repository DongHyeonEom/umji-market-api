package com.buyeong.umji.api.account.adapter.`in`.web

import com.buyeong.umji.api.account.application.model.BuyerGroupInvitation
import com.buyeong.umji.api.account.application.model.BuyerGroupJoinRequest
import com.buyeong.umji.api.account.application.model.BuyerGroupSearchResult
import com.buyeong.umji.api.account.application.model.BuyerGroupSummary
import com.buyeong.umji.api.account.application.model.BuyerGroupRegistrationCommand
import com.buyeong.umji.api.account.application.port.`in`.BuyerGroupMembershipUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TransactionalBuyerGroupMembershipUseCase(private val groups: BuyerGroupMembershipUseCase) {
    @Transactional(readOnly = true)
    fun current(accountId: UUID): BuyerGroupSummary? = groups.current(accountId)

    @Transactional
    fun createIndividualGroup(accountId: UUID, name: String): BuyerGroupSummary = groups.createIndividualGroup(accountId, name)

    fun register(accountId: UUID, command: BuyerGroupRegistrationCommand): BuyerGroupSummary = groups.register(accountId, command)

    @Transactional(readOnly = true)
    fun search(phone: String): List<BuyerGroupSearchResult> = groups.search(phone)

    @Transactional
    fun invite(accountId: UUID, phone: String): BuyerGroupInvitation = groups.invite(accountId, phone)

    @Transactional(readOnly = true)
    fun invitations(accountId: UUID): List<BuyerGroupInvitation> = groups.invitations(accountId)

    @Transactional
    fun respondInvitation(accountId: UUID, invitationId: UUID, accept: Boolean) = groups.respondInvitation(accountId, invitationId, accept)

    @Transactional
    fun requestToJoin(accountId: UUID, groupId: UUID) = groups.requestToJoin(accountId, groupId)

    @Transactional(readOnly = true)
    fun pendingJoinRequests(accountId: UUID): List<BuyerGroupJoinRequest> = groups.pendingJoinRequests(accountId)

    @Transactional
    fun respondJoinRequest(accountId: UUID, requestId: UUID, approve: Boolean) = groups.respondJoinRequest(accountId, requestId, approve)
}
