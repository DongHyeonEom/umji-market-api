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
        members.findFirstByAccount_Id(accountId)?.let { membership ->
            if (membership.status == ACTIVE && membership.buyerGroup.status == ACTIVE) return membership.buyerGroup
            throw IllegalStateException("계정의 구매자 그룹 소속이 비활성 상태입니다.")
        }

        val businessProfile = accounts.profile(accountId)
        val group = groups.saveAndFlush(
            BuyerGroupEntity().apply {
                groupType = if (businessProfile == null) INDIVIDUAL else BUSINESS
                displayName = businessProfile?.businessName ?: account.name
                status = ACTIVE
            },
        )
        members.saveAndFlush(
            BuyerGroupMemberEntity().apply {
                buyerGroup = group
                this.account = account
                status = ACTIVE
            },
        )
        businessProfile?.let { saveBusinessProfile(group, it) }
        return group
    }

    @Transactional
    fun assignAccountToBusinessGroup(accountPublicId: UUID, buyerGroupPublicId: UUID) {
        val account = accounts.findByPublicId(accountPublicId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val group = groups.findByPublicId(buyerGroupPublicId) ?: throw ItemNotFoundException("구매자 그룹을 찾을 수 없습니다.")
        require(group.groupType == BUSINESS && group.status == ACTIVE) { "활성 사업자 구매자 그룹만 지정할 수 있습니다." }

        val accountId = requireNotNull(account.id)
        if (members.findFirstByAccount_Id(accountId) == null) ensureForAccount(accountPublicId)
        val membership = members.findFirstByAccount_Id(accountId)
            ?: throw IllegalStateException("계정의 구매자 그룹 소속을 생성하지 못했습니다.")
        check(membership.status == ACTIVE) { "비활성 구매자 그룹 구성원은 재배정할 수 없습니다." }
        membership.buyerGroup = group
        members.saveAndFlush(membership)
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