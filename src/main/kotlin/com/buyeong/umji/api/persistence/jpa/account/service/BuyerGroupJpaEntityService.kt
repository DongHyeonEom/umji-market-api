package com.buyeong.umji.api.persistence.jpa.account

import com.buyeong.umji.api.exception.ItemNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BuyerGroupJpaEntityService(
    private val accounts: AccountJpaEntityService,
    private val groups: BuyerGroupRepository,
    private val members: BuyerGroupMemberRepository,
    private val businessProfiles: BuyerGroupBusinessProfileRepository,
) {
    fun activeForAccount(accountId: Long): BuyerGroupEntity? =
        members.findFirstByAccount_IdAndStatus(accountId, ACTIVE)?.buyerGroup?.takeIf { it.status == ACTIVE }

    fun activeForAccountPublicId(accountPublicId: UUID): BuyerGroupEntity? =
        accounts.findByPublicId(accountPublicId)?.id?.let(::activeForAccount)

    @Transactional
    fun lockActiveForAccountPublicId(accountPublicId: UUID): BuyerGroupEntity {
        val active = activeForAccountPublicId(accountPublicId)
            ?: throw ItemNotFoundException("활성 구매자 그룹을 찾을 수 없습니다.")
        return groups.findLockedById(requireNotNull(active.id))
            ?: throw ItemNotFoundException("활성 구매자 그룹을 찾을 수 없습니다.")
    }

    @Transactional
    fun ensureForAccount(accountPublicId: UUID): BuyerGroupEntity {
        val account = accounts.findByPublicId(accountPublicId)
            ?: throw IllegalArgumentException("계정을 찾을 수 없습니다.")
        val accountId = requireNotNull(account.id)
        members.findFirstByAccount_IdAndStatusOrderByJoinedAtDesc(accountId, ACTIVE)
            ?.let { membership -> if (membership.buyerGroup.status == ACTIVE) return membership.buyerGroup }

        val businessProfile = accounts.profile(accountId)
        val group = groups.saveAndFlush(
            BuyerGroupEntity().apply {
                groupType = if (businessProfile == null) INDIVIDUAL else BUSINESS
                displayName = businessProfile?.businessName ?: account.name
                status = ACTIVE
                representativeAccount = account
            },
        )
        val targetMembership = members.findFirstByBuyerGroup_IdAndAccount_Id(requireNotNull(group.id), accountId)
        if (targetMembership == null) {
            members.saveAndFlush(
                BuyerGroupMemberEntity().apply {
                    buyerGroup = group
                    this.account = account
                    status = ACTIVE
                },
            )
        } else {
            targetMembership.status = ACTIVE
            targetMembership.joinedAt = java.time.Instant.now()
            members.saveAndFlush(targetMembership)
        }
        businessProfile?.let { saveBusinessProfile(group, it) }
        return group
    }

    @Transactional
    fun assignAccountToBusinessGroup(accountPublicId: UUID, buyerGroupPublicId: UUID) {
        val account = accounts.findByPublicId(accountPublicId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val group = groups.findByPublicId(buyerGroupPublicId) ?: throw ItemNotFoundException("구매자 그룹을 찾을 수 없습니다.")
        require(group.groupType == BUSINESS && group.status == ACTIVE) { "활성 사업자 구매자 그룹만 지정할 수 있습니다." }
        val lockedGroup = groups.findLockedById(requireNotNull(group.id))
            ?: throw ItemNotFoundException("구매자 그룹을 찾을 수 없습니다.")

        val accountId = requireNotNull(account.id)
        val membership = members.findFirstByAccount_IdAndStatusOrderByJoinedAtDesc(accountId, ACTIVE)
        if (membership?.buyerGroup?.id == group.id) return
        membership?.let {
            val previousGroup = groups.findLockedById(requireNotNull(it.buyerGroup.id))
                ?: throw ItemNotFoundException("기존 구매자 그룹을 찾을 수 없습니다.")
            if (previousGroup.representativeAccount?.id == accountId) {
                val currentMembers = members.findAllByBuyerGroup_IdAndStatus(requireNotNull(previousGroup.id), ACTIVE)
                check(currentMembers.size == 1) {
                    "그룹 대표자를 다른 구성원으로 변경한 뒤 계정을 이동해야 합니다."
                }
                previousGroup.representativeAccount = null
                previousGroup.status = "INACTIVE"
            }
            it.status = "LEFT"
            members.saveAndFlush(it)
        }
        members.saveAndFlush(
            BuyerGroupMemberEntity().apply {
                buyerGroup = lockedGroup
                this.account = account
                status = ACTIVE
            },
        )
    }

    @Transactional
    fun setRepresentative(groupPublicId: UUID, accountPublicId: UUID) {
        val group = groups.findByPublicId(groupPublicId)?.takeIf { it.status == ACTIVE }
            ?: throw ItemNotFoundException("활성 구매자 그룹을 찾을 수 없습니다.")
        val lockedGroup = groups.findLockedById(requireNotNull(group.id))?.takeIf { it.status == ACTIVE }
            ?: throw ItemNotFoundException("활성 구매자 그룹을 찾을 수 없습니다.")
        val account = accounts.findByPublicId(accountPublicId) ?: throw ItemNotFoundException("대표자로 지정할 계정을 찾을 수 없습니다.")
        require(members.findFirstByAccount_IdAndStatusOrderByJoinedAtDesc(requireNotNull(account.id), ACTIVE)?.buyerGroup?.id == lockedGroup.id) {
            "대표자는 해당 그룹의 활성 구성원이어야 합니다."
        }
        lockedGroup.representativeAccount = account
    }

    private fun saveBusinessProfile(group: BuyerGroupEntity, profile: BusinessProfileEntity) {
        businessProfiles.save(
            BuyerGroupBusinessProfileEntity().apply {
                buyerGroup = group
                businessName = profile.businessName
                businessRegistrationNumber = profile.businessRegistrationNumber
                representativeName = profile.representativeName
                businessPhone = profile.businessPhone
                postalCode = profile.postalCode
                address1 = profile.address1
                address2 = profile.address2
                status = profile.status
            },
        )
    }

    private companion object {
        const val ACTIVE = "ACTIVE"
        const val BUSINESS = "BUSINESS"
        const val INDIVIDUAL = "INDIVIDUAL"
    }
}