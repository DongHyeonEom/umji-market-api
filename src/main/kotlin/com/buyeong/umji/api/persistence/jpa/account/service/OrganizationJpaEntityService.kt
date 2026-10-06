package com.buyeong.umji.api.persistence.jpa.account.service

import com.buyeong.umji.api.exception.ForbiddenOperationException
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.operation.account.model.OrganizationProfileData
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationBusinessProfileEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationMemberEntity
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationBusinessProfileRepository
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationCapabilityRepository
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationMemberRepository
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrganizationJpaEntityService(
    private val accounts: AccountJpaEntityService,
    private val groups: OrganizationRepository,
    private val members: OrganizationMemberRepository,
    private val organizationProfiles: OrganizationBusinessProfileRepository,
    private val capabilities: OrganizationCapabilityRepository,
) {
    fun activeForAccount(accountId: Long): OrganizationEntity? =
        members.findFirstByAccount_IdAndStatus(accountId, ACTIVE)?.organization?.takeIf { it.status == ACTIVE }

    fun activeForAccountPublicId(accountPublicId: UUID): OrganizationEntity? =
        accounts.findByPublicId(accountPublicId)?.id?.let(::activeForAccount)

    fun profileForAccount(accountPublicId: UUID): OrganizationBusinessProfileEntity? =
        activeForAccountPublicId(accountPublicId)?.id?.let(organizationProfiles::findByOrganization_Id)

    fun hasCapability(organizationId: UUID, capability: String): Boolean =
        groups.findByPublicId(organizationId)?.id?.let { capabilities.existsByOrganization_IdAndCapabilityCode(it, capability) } ?: false

    fun capabilitiesForOrganization(organizationId: Long): Set<String> =
        capabilities.findAllByOrganization_Id(organizationId).mapTo(linkedSetOf()) { it.capabilityCode }

    fun activeBuyerForAccountPublicId(accountPublicId: UUID): OrganizationEntity? =
        activeForAccountPublicId(accountPublicId)?.takeIf { hasCapability(requireNotNull(it.publicId), BUYER) }

    fun defaultTaxInvoiceRequestedForAccount(accountPublicId: UUID): Boolean =
        activeBuyerForAccountPublicId(accountPublicId)?.defaultTaxInvoiceRequested ?: false

    @Transactional
    fun updateDefaultTaxInvoiceRequestedForAccount(accountPublicId: UUID, requested: Boolean) {
        val organization = activeBuyerForAccountPublicId(accountPublicId)
            ?: throw ItemNotFoundException("활성 구매 Organization을 찾을 수 없습니다.")
        if (organization.representativeAccount?.publicId != accountPublicId) {
            throw ForbiddenOperationException("Organization 대표자만 세금계산서 기본 발행 설정을 변경할 수 있습니다.")
        }
        organization.defaultTaxInvoiceRequested = requested
        groups.save(organization)
    }

    @Transactional
    fun updateBusinessProfileForAccount(accountPublicId: UUID, source: OrganizationProfileData) {
        val organization = ensureForAccount(accountPublicId)
        val profile = organizationProfiles.findByOrganization_Id(requireNotNull(organization.id))
            ?: OrganizationBusinessProfileEntity().apply {
                this.organization = organization
                businessName = source.businessName
                status = source.status
            }
        val registrationNumber = source.businessRegistrationNumber?.trim()?.ifBlank { null }
        if (profile.businessRegistrationNumber != registrationNumber) {
            profile.businessRegistrationVerificationStatus = if (registrationNumber == null) "NOT_REQUIRED" else "PENDING"
            profile.businessRegistrationVerifiedAt = null
            profile.businessRegistrationConfirmedAt = null
        }
        profile.businessName = source.businessName
        profile.businessRegistrationNumber = registrationNumber
        profile.representativeName = source.representativeName
        profile.businessPhone = source.businessPhone
        profile.postalCode = source.postalCode
        profile.address1 = source.address1
        profile.address2 = source.address2
        profile.status = source.status
        organizationProfiles.save(profile)
    }

    @Transactional
    fun lockActiveForAccountPublicId(accountPublicId: UUID): OrganizationEntity {
        val active = activeForAccountPublicId(accountPublicId)
            ?: throw ItemNotFoundException("활성 구매자 그룹을 찾을 수 없습니다.")
        require(capabilities.existsByOrganization_IdAndCapabilityCode(requireNotNull(active.id), BUYER)) {
            "구매 capability가 있는 Organization이 아닙니다."
        }
        return groups.findLockedById(requireNotNull(active.id))
            ?: throw ItemNotFoundException("활성 구매자 그룹을 찾을 수 없습니다.")
    }

    @Transactional
    fun ensureForAccount(
        accountPublicId: UUID,
        capability: String = BUYER,
        profileData: OrganizationProfileData? = null,
    ): OrganizationEntity {
        require(capability in setOf(BUYER, SELLER, OPERATOR)) { "Organization capability가 올바르지 않습니다." }
        val account = accounts.findByPublicId(accountPublicId)
            ?: throw IllegalArgumentException("계정을 찾을 수 없습니다.")
        val accountId = requireNotNull(account.id)
        members.findFirstByAccount_IdAndStatusOrderByJoinedAtDesc(accountId, ACTIVE)
            ?.let { membership -> if (membership.organization.status == ACTIVE) return membership.organization }

        val group = groups.saveAndFlush(
            OrganizationEntity().apply {
                organizationType = if (profileData == null) INDIVIDUAL else BUSINESS
                displayName = profileData?.businessName ?: account.name
                status = ACTIVE
                representativeAccount = account
            },
        )
        val targetMembership = members.findFirstByOrganization_IdAndAccount_Id(requireNotNull(group.id), accountId)
        if (targetMembership == null) {
            members.saveAndFlush(
                OrganizationMemberEntity().apply {
                    organization = group
                    this.account = account
                    status = ACTIVE
                },
            )
        } else {
            targetMembership.status = ACTIVE
            targetMembership.joinedAt = java.time.Instant.now()
            members.saveAndFlush(targetMembership)
        }
        profileData?.let { saveBusinessProfile(group, it) }
        capabilities.save(com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationCapabilityEntity().apply {
            organization = group
            capabilityCode = capability
        })
        return group
    }

    @Transactional
    fun assignAccountToOrganization(accountPublicId: UUID, organizationPublicId: UUID) {
        val account = accounts.findByPublicId(accountPublicId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val group = groups.findByPublicId(organizationPublicId) ?: throw ItemNotFoundException("구매자 그룹을 찾을 수 없습니다.")
        require(group.status == ACTIVE && (capabilities.existsByOrganization_IdAndCapabilityCode(requireNotNull(group.id), BUYER)
            || capabilities.existsByOrganization_IdAndCapabilityCode(requireNotNull(group.id), SELLER))) {
            "활성 구매 또는 판매 Organization만 지정할 수 있습니다."
        }
        val lockedGroup = groups.findLockedById(requireNotNull(group.id))
            ?: throw ItemNotFoundException("구매자 그룹을 찾을 수 없습니다.")

        val accountId = requireNotNull(account.id)
        val membership = members.findFirstByAccount_IdAndStatusOrderByJoinedAtDesc(accountId, ACTIVE)
        if (membership?.organization?.id == group.id) return
        membership?.let {
            val previousGroup = groups.findLockedById(requireNotNull(it.organization.id))
                ?: throw ItemNotFoundException("기존 구매자 그룹을 찾을 수 없습니다.")
            if (previousGroup.representativeAccount?.id == accountId) {
                val currentMembers = members.findAllByOrganization_IdAndStatus(requireNotNull(previousGroup.id), ACTIVE)
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
            OrganizationMemberEntity().apply {
                organization = lockedGroup
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
        require(members.findFirstByAccount_IdAndStatusOrderByJoinedAtDesc(requireNotNull(account.id), ACTIVE)?.organization?.id == lockedGroup.id) {
            "대표자는 해당 그룹의 활성 구성원이어야 합니다."
        }
        lockedGroup.representativeAccount = account
    }

    private fun saveBusinessProfile(group: OrganizationEntity, profile: OrganizationProfileData) {
        val existing = organizationProfiles.findByOrganization_Id(requireNotNull(group.id))
        val target = existing ?: OrganizationBusinessProfileEntity().apply { organization = group }
        target.businessName = profile.businessName
        val registrationNumber = profile.businessRegistrationNumber?.trim()?.ifBlank { null }
        target.businessRegistrationNumber = registrationNumber
        target.businessRegistrationVerificationStatus = if (registrationNumber == null) "NOT_REQUIRED" else "PENDING"
        target.representativeName = profile.representativeName
        target.businessPhone = profile.businessPhone
        target.postalCode = profile.postalCode
        target.address1 = profile.address1
        target.address2 = profile.address2
        target.status = profile.status
        organizationProfiles.save(target)
    }

    private companion object {
        const val BUYER = "BUYER"
        const val SELLER = "SELLER"
        const val OPERATOR = "OPERATOR"
        const val ACTIVE = "ACTIVE"
        const val BUSINESS = "BUSINESS"
        const val INDIVIDUAL = "INDIVIDUAL"
    }
}
