package com.buyeong.umji.api.account.adapter.out.persistence

import com.buyeong.umji.api.account.application.model.BuyerGroupInvitation
import com.buyeong.umji.api.account.application.model.BuyerGroupJoinRequest
import com.buyeong.umji.api.account.application.model.BuyerGroupSearchResult
import com.buyeong.umji.api.account.application.model.BuyerGroupSummary
import com.buyeong.umji.api.account.application.port.out.BuyerGroupMembershipPort
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupEntity
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupInvitationEntity
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupInvitationRepository
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupJoinRequestEntity
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupJoinRequestRepository
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupMemberEntity
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupMemberRepository
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class JpaBuyerGroupMembershipAdapter(
    private val accounts: AccountJpaEntityService,
    private val groups: BuyerGroupRepository,
    private val members: BuyerGroupMemberRepository,
    private val invitations: BuyerGroupInvitationRepository,
    private val joinRequests: BuyerGroupJoinRequestRepository,
    private val buyerGroupEntities: BuyerGroupJpaEntityService,
) : BuyerGroupMembershipPort {
    @Transactional(readOnly = true)
    override fun current(accountId: UUID): BuyerGroupSummary? {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val membership = members.findFirstByAccount_IdAndStatusOrderByJoinedAtDesc(requireNotNull(account.id), ACTIVE) ?: return null
        val group = membership.buyerGroup.takeIf { it.status == ACTIVE } ?: return null
        return group.toSummary(account)
    }

    @Transactional
    override fun createIndividualGroup(accountId: UUID, name: String): BuyerGroupSummary {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        require(members.findFirstByAccount_IdAndStatus(requireNotNull(account.id), ACTIVE) == null) { "이미 활성 구매자 그룹에 소속되어 있습니다." }
        val group = groups.saveAndFlush(
            BuyerGroupEntity().apply {
                groupType = INDIVIDUAL
                displayName = name
                status = ACTIVE
                representativeAccount = account
            },
        )
        members.saveAndFlush(
            BuyerGroupMemberEntity().apply {
                buyerGroup = group
                this.account = account
                status = ACTIVE
            },
        )
        return group.toSummary(account)
    }

    @Transactional(readOnly = true)
    override fun search(phoneNormalized: String): List<BuyerGroupSearchResult> =
        groups.searchByPhone(phoneNormalized).map { BuyerGroupSearchResult(requireNotNull(it.publicId), it.groupType, it.displayName) }

    @Transactional
    override fun invite(accountId: UUID, phoneNormalized: String): BuyerGroupInvitation {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val group = representativeGroup(account) ?: throw ItemNotFoundException("대표자 권한이 있는 활성 그룹을 찾을 수 없습니다.")
        lockGroup(requireNotNull(group.id))
        require(account.phoneNormalized != phoneNormalized) { "본인 휴대폰 번호는 초대할 수 없습니다." }
        require(members.findAllByBuyerGroup_IdAndStatus(requireNotNull(group.id), ACTIVE).none { it.account.phoneNormalized == phoneNormalized }) {
            "이미 그룹 구성원인 휴대폰 번호입니다."
        }
        val existing = invitations.findFirstByBuyerGroup_IdAndPhoneNormalizedAndStatus(requireNotNull(group.id), phoneNormalized, PENDING)
        val invitation = existing ?: invitations.save(
            BuyerGroupInvitationEntity().apply {
                buyerGroup = group
                this.phoneNormalized = phoneNormalized
                invitedBy = account
                targetAccount = accounts.findByPhoneNormalized(phoneNormalized)
                status = PENDING
            },
        )
        return invitation.toModel()
    }

    @Transactional(readOnly = true)
    override fun invitations(accountId: UUID): List<BuyerGroupInvitation> {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val phone = account.phoneNormalized ?: return emptyList()
        return invitations.findAllByPhoneNormalizedAndStatus(phone, PENDING)
            .filter { it.buyerGroup.status == ACTIVE }
            .map { it.toModel() }
    }

    @Transactional
    override fun respondInvitation(accountId: UUID, invitationId: UUID, accept: Boolean) {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val phone = account.phoneNormalized ?: throw IllegalStateException("계정 휴대폰 번호를 확인할 수 없습니다.")
        val invitation = invitations.findAllByPhoneNormalizedAndStatus(phone, PENDING)
            .firstOrNull { it.publicId == invitationId }
            ?: throw ItemNotFoundException("대기 중인 그룹 초대를 찾을 수 없습니다.")
        val group = lockGroup(requireNotNull(invitation.buyerGroup.id))
        if (accept) {
            moveIntoGroup(account, group)
            invitation.targetAccount = account
            invitation.status = ACCEPTED
        } else {
            invitation.status = DECLINED
        }
        invitation.respondedAt = Instant.now()
    }

    @Transactional
    override fun requestToJoin(accountId: UUID, groupId: UUID) {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val group = groups.findByPublicId(groupId)?.takeIf { it.status == ACTIVE }
            ?: throw ItemNotFoundException("활성 구매자 그룹을 찾을 수 없습니다.")
        lockGroup(requireNotNull(group.id))
        val accountIdValue = requireNotNull(account.id)
        require(members.findFirstByAccount_IdAndStatus(accountIdValue, ACTIVE)?.buyerGroup?.id != group.id) { "이미 해당 그룹의 구성원입니다." }
        val currentGroup = members.findFirstByAccount_IdAndStatus(accountIdValue, ACTIVE)?.buyerGroup
        require(currentGroup?.representativeAccount?.id != accountIdValue || currentGroup.groupType == INDIVIDUAL) {
            "사업자 그룹 대표자는 그룹 가입 요청 전에 운영자에게 대표자 변경을 요청해 주세요."
        }
        require(joinRequests.findFirstByBuyerGroup_IdAndAccount_IdAndStatus(requireNotNull(group.id), accountIdValue, PENDING) == null) {
            "이미 가입 요청을 보냈습니다."
        }
        joinRequests.save(
            BuyerGroupJoinRequestEntity().apply {
                buyerGroup = group
                this.account = account
                status = PENDING
            },
        )
    }

    @Transactional(readOnly = true)
    override fun pendingJoinRequests(accountId: UUID): List<BuyerGroupJoinRequest> {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val group = representativeGroup(account) ?: return emptyList()
        return joinRequests.findAllByBuyerGroup_IdAndStatusOrderByRequestedAtAsc(requireNotNull(group.id), PENDING).map { it.toModel() }
    }

    @Transactional
    override fun respondJoinRequest(accountId: UUID, requestId: UUID, approve: Boolean) {
        val representative = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val request = joinRequests.findFirstByPublicIdAndStatus(requestId, PENDING)
            ?: throw ItemNotFoundException("대기 중인 가입 요청을 찾을 수 없습니다.")
        val group = lockGroup(requireNotNull(request.buyerGroup.id))
        require(group.representativeAccount?.id == representative.id) { "해당 그룹 대표자만 가입 요청을 처리할 수 있습니다." }
        if (approve) moveIntoGroup(request.account, group)
        request.status = if (approve) APPROVED else DECLINED
        request.respondedAt = Instant.now()
        request.respondedBy = representative
    }

    private fun representativeGroup(account: AccountEntity): BuyerGroupEntity? {
        val membership = members.findFirstByAccount_IdAndStatus(requireNotNull(account.id), ACTIVE) ?: return null
        return membership.buyerGroup.takeIf { it.status == ACTIVE && it.representativeAccount?.id == account.id }
    }

    private fun moveIntoGroup(account: AccountEntity, target: BuyerGroupEntity) {
        val accountId = requireNotNull(account.id)
        val current = members.findFirstByAccount_IdAndStatusOrderByJoinedAtDesc(accountId, ACTIVE)
        if (current?.buyerGroup?.id == target.id) return
        current?.let {
            val oldGroup = lockGroup(requireNotNull(it.buyerGroup.id))
            if (oldGroup.representativeAccount?.id == accountId) {
                require(oldGroup.groupType == INDIVIDUAL) { "사업자 그룹 대표자는 운영자만 소속을 변경할 수 있습니다." }
                require(members.findAllByBuyerGroup_IdAndStatus(requireNotNull(oldGroup.id), ACTIVE).size == 1) {
                    "다른 구성원이 있는 개인 그룹 대표자는 운영자에게 대표자 변경을 요청해 주세요."
                }
                oldGroup.representativeAccount = null
                oldGroup.status = INACTIVE
            }
            it.status = LEFT
            members.saveAndFlush(it)
        }
        val existingTarget = members.findFirstByBuyerGroup_IdAndAccount_Id(requireNotNull(target.id), accountId)
        if (existingTarget == null) {
            members.saveAndFlush(
                BuyerGroupMemberEntity().apply {
                    buyerGroup = target
                    this.account = account
                    status = ACTIVE
                },
            )
        } else {
            existingTarget.status = ACTIVE
            existingTarget.joinedAt = Instant.now()
            members.saveAndFlush(existingTarget)
        }
    }

    private fun lockGroup(id: Long): BuyerGroupEntity =
        groups.findLockedById(id)?.takeIf { it.status == ACTIVE }
            ?: throw ItemNotFoundException("활성 구매자 그룹을 찾을 수 없습니다.")

    private fun BuyerGroupEntity.toSummary(account: AccountEntity) =
        BuyerGroupSummary(requireNotNull(publicId), groupType, displayName, representativeAccount?.id == account.id)

    private fun BuyerGroupInvitationEntity.toModel() = BuyerGroupInvitation(
        requireNotNull(publicId),
        requireNotNull(buyerGroup.publicId),
        buyerGroup.displayName,
        phoneNormalized.takeLast(4).padStart(phoneNormalized.length, '*'),
    )

    private fun BuyerGroupJoinRequestEntity.toModel() = BuyerGroupJoinRequest(
        requireNotNull(publicId),
        requireNotNull(buyerGroup.publicId),
        buyerGroup.displayName,
        account.name,
        account.phoneNormalized?.let { "***-****-${it.takeLast(4)}" } ?: "",
        requestedAt,
    )

    private companion object {
        const val ACTIVE = "ACTIVE"
        const val LEFT = "LEFT"
        const val PENDING = "PENDING"
        const val ACCEPTED = "ACCEPTED"
        const val DECLINED = "DECLINED"
        const val APPROVED = "APPROVED"
        const val INDIVIDUAL = "INDIVIDUAL"
        const val INACTIVE = "INACTIVE"
    }
}