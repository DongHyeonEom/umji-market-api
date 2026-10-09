package com.buyeong.umji.api.persistence.jpa.account.service

import com.buyeong.umji.api.domain.account.dto.OrganizationInvitationDto
import com.buyeong.umji.api.domain.account.dto.OrganizationRegistrationCommandDto
import com.buyeong.umji.api.domain.account.dto.OrganizationSearchResultDto
import com.buyeong.umji.api.domain.account.dto.OrganizationSummaryDto
import com.buyeong.umji.api.domain.account.model.OrganizationJoinRequest
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationBusinessProfileEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationCapabilityEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationInvitationEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationJoinRequestEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationMemberEntity
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationBusinessProfileRepository
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationCapabilityRepository
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationInvitationRepository
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationJoinRequestRepository
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationMemberRepository
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OrganizationMembershipJpaEntityService(
    private val accounts: AccountJpaEntityService,
    private val groups: OrganizationRepository,
    private val members: OrganizationMemberRepository,
    private val invitations: OrganizationInvitationRepository,
    private val joinRequests: OrganizationJoinRequestRepository,
    private val organizationEntities: OrganizationJpaEntityService,
    private val organizationProfiles: OrganizationBusinessProfileRepository,
    private val capabilities: OrganizationCapabilityRepository,
) {
    fun current(accountId: UUID): OrganizationSummaryDto? {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val membership = members.findFirstByAccount_IdAndStatusOrderByJoinedAtDesc(requireNotNull(account.id), ACTIVE) ?: return null
        val group = membership.organization.takeIf { it.status == ACTIVE } ?: return null
        return group.toSummary(account)
    }

    @Transactional
    fun createIndividualGroup(accountId: UUID, name: String): OrganizationSummaryDto {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        require(members.findFirstByAccount_IdAndStatus(requireNotNull(account.id), ACTIVE) == null) { "이미 활성 구매자 그룹에 소속되어 있습니다." }
        val group = groups.saveAndFlush(
            OrganizationEntity().apply {
                organizationType = INDIVIDUAL
                displayName = name
                status = ACTIVE
                representativeAccount = account
            },
        )
        members.saveAndFlush(
            OrganizationMemberEntity().apply {
                organization = group
                this.account = account
                status = ACTIVE
            },
        )
        capabilities.save(
            OrganizationCapabilityEntity().apply {
                organization = group
                capabilityCode = BUYER
            }
        )
        return group.toSummary(account)
    }

    @Transactional
    fun register(accountId: UUID, command: OrganizationRegistrationCommandDto): OrganizationSummaryDto {
        val account = accounts.lockByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val accountInternalId = requireNotNull(account.id)
        require(members.findFirstByAccount_IdAndStatus(accountInternalId, ACTIVE) == null) { "이미 활성 구매자 그룹에 소속되어 있습니다." }
        require(command.capability == BUYER || command.capability == SELLER) { "Organization capability는 BUYER 또는 SELLER여야 합니다." }
        val business = command.business
        val organizationType = when (command.type) {
            INDIVIDUAL -> {
                require(business == null) { "개인 그룹 등록에는 사업자등록 정보가 없어야 합니다." }
                INDIVIDUAL
            }
            BUSINESS -> {
                requireNotNull(business) { "사업자 그룹 등록 정보가 필요합니다." }
                BUSINESS
            }
            else -> throw IllegalArgumentException("지원하지 않는 그룹 유형입니다.")
        }
        val group = groups.saveAndFlush(
            OrganizationEntity().apply {
                this.organizationType = organizationType
                displayName = business?.businessName?.trim() ?: account.name
                status = ACTIVE
                representativeAccount = account
            },
        )
        members.saveAndFlush(
            OrganizationMemberEntity().apply {
                organization = group
                this.account = account
                status = ACTIVE
            },
        )
        capabilities.save(
            OrganizationCapabilityEntity().apply {
                organization = group
                capabilityCode = command.capability
            }
        )
        if (business != null) {
            organizationProfiles.save(
                OrganizationBusinessProfileEntity().apply {
                    organization = group
                    businessName = business.businessName.trim()
                    businessRegistrationNumber = business.businessRegistrationNumber
                    representativeName = business.representativeName.trim()
                    postalCode = business.postalCode.trim()
                    address1 = business.address1.trim()
                    address2 = business.address2?.trim()?.ifBlank { null }
                    businessIndustry = business.businessIndustry.trim()
                    businessItem = business.businessItem.trim()
                    taxInvoiceEmail = business.email?.trim()?.ifBlank { null }
                    status = COMPLETED
                    businessRegistrationVerifiedAt = Instant.now()
                    businessRegistrationVerificationStatus = "ACTIVE"
                    businessRegistrationConfirmedAt = Instant.now()
                },
            )
        }
        return group.toSummary(account)
    }

    fun search(phoneNormalized: String, capability: String): List<OrganizationSearchResultDto> =
        groups.searchByPhone(phoneNormalized).filter { organization ->
            capabilities.existsByOrganization_IdAndCapabilityCode(requireNotNull(organization.id), capability)
        }.map { organization ->
            OrganizationSearchResultDto(
                requireNotNull(organization.publicId),
                organization.organizationType,
                organization.displayName,
                capabilities.findAllByOrganization_Id(requireNotNull(organization.id)).mapTo(linkedSetOf()) { it.capabilityCode },
            )
        }

    @Transactional
    fun invite(accountId: UUID, phoneNormalized: String): OrganizationInvitationDto {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val group = representativeGroup(account) ?: throw ItemNotFoundException("대표자 권한이 있는 활성 그룹을 찾을 수 없습니다.")
        require(
            capabilities.existsByOrganization_IdAndCapabilityCode(requireNotNull(group.id), BUYER) ||
                capabilities.existsByOrganization_IdAndCapabilityCode(requireNotNull(group.id), SELLER)
        ) {
            "구성원을 초대할 수 있는 Organization이 아닙니다."
        }
        lockGroup(requireNotNull(group.id))
        require(account.phoneNormalized != phoneNormalized) { "본인 휴대폰 번호는 초대할 수 없습니다." }
        require(members.findAllByOrganization_IdAndStatus(requireNotNull(group.id), ACTIVE).none { it.account.phoneNormalized == phoneNormalized }) {
            "이미 그룹 구성원인 휴대폰 번호입니다."
        }
        val existing = invitations.findFirstByOrganization_IdAndPhoneNormalizedAndStatus(requireNotNull(group.id), phoneNormalized, PENDING)
        val invitation = existing ?: invitations.save(
            OrganizationInvitationEntity().apply {
                organization = group
                this.phoneNormalized = phoneNormalized
                invitedBy = account
                targetAccount = accounts.findByPhoneNormalized(phoneNormalized)
                status = PENDING
            },
        )
        return invitation.toModel()
    }

    fun invitations(accountId: UUID): List<OrganizationInvitationDto> {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val phone = account.phoneNormalized ?: return emptyList()
        return invitations.findAllByPhoneNormalizedAndStatus(phone, PENDING)
            .filter { it.organization.status == ACTIVE }
            .map { it.toModel() }
    }

    @Transactional
    fun respondInvitation(accountId: UUID, invitationId: UUID, accept: Boolean) {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val phone = account.phoneNormalized ?: throw IllegalStateException("계정 휴대폰 번호를 확인할 수 없습니다.")
        val invitation = invitations.findAllByPhoneNormalizedAndStatus(phone, PENDING)
            .firstOrNull { it.publicId == invitationId }
            ?: throw ItemNotFoundException("대기 중인 그룹 초대를 찾을 수 없습니다.")
        val group = lockGroup(requireNotNull(invitation.organization.id))
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
    fun requestToJoin(accountId: UUID, organizationId: UUID) {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val group = groups.findByPublicId(organizationId)?.takeIf { it.status == ACTIVE }
            ?: throw ItemNotFoundException("활성 구매자 그룹을 찾을 수 없습니다.")
        require(
            capabilities.existsByOrganization_IdAndCapabilityCode(requireNotNull(group.id), BUYER) ||
                capabilities.existsByOrganization_IdAndCapabilityCode(requireNotNull(group.id), SELLER)
        ) {
            "구성원 가입을 허용하지 않는 Organization입니다."
        }
        lockGroup(requireNotNull(group.id))
        val accountIdValue = requireNotNull(account.id)
        require(members.findFirstByAccount_IdAndStatus(accountIdValue, ACTIVE)?.organization?.id != group.id) { "이미 해당 그룹의 구성원입니다." }
        val currentOrganization = members.findFirstByAccount_IdAndStatus(accountIdValue, ACTIVE)?.organization
        require(currentOrganization?.representativeAccount?.id != accountIdValue || currentOrganization.organizationType == INDIVIDUAL) {
            "사업자 그룹 대표자는 그룹 가입 요청 전에 운영자에게 대표자 변경을 요청해 주세요."
        }
        require(joinRequests.findFirstByOrganization_IdAndAccount_IdAndStatus(requireNotNull(group.id), accountIdValue, PENDING) == null) {
            "이미 가입 요청을 보냈습니다."
        }
        joinRequests.save(
            OrganizationJoinRequestEntity().apply {
                organization = group
                this.account = account
                status = PENDING
            },
        )
    }

    fun pendingJoinRequests(accountId: UUID): List<OrganizationJoinRequest> {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val group = representativeGroup(account) ?: return emptyList()
        return joinRequests.findAllByOrganization_IdAndStatusOrderByRequestedAtAsc(requireNotNull(group.id), PENDING).map { it.toModel() }
    }

    @Transactional
    fun respondJoinRequest(accountId: UUID, requestId: UUID, approve: Boolean) {
        val representative = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val request = joinRequests.findFirstByPublicIdAndStatus(requestId, PENDING)
            ?: throw ItemNotFoundException("대기 중인 가입 요청을 찾을 수 없습니다.")
        val group = lockGroup(requireNotNull(request.organization.id))
        require(group.representativeAccount?.id == representative.id) { "해당 그룹 대표자만 가입 요청을 처리할 수 있습니다." }
        if (approve) moveIntoGroup(request.account, group)
        request.status = if (approve) APPROVED else DECLINED
        request.respondedAt = Instant.now()
        request.respondedBy = representative
    }

    private fun representativeGroup(account: AccountEntity): OrganizationEntity? {
        val membership = members.findFirstByAccount_IdAndStatus(requireNotNull(account.id), ACTIVE) ?: return null
        return membership.organization.takeIf { it.status == ACTIVE && it.representativeAccount?.id == account.id }
    }

    private fun moveIntoGroup(account: AccountEntity, target: OrganizationEntity) {
        val accountId = requireNotNull(account.id)
        val current = members.findFirstByAccount_IdAndStatusOrderByJoinedAtDesc(accountId, ACTIVE)
        if (current?.organization?.id == target.id) return
        current?.let {
            val oldGroup = lockGroup(requireNotNull(it.organization.id))
            if (oldGroup.representativeAccount?.id == accountId) {
                require(oldGroup.organizationType == INDIVIDUAL) { "사업자 그룹 대표자는 운영자만 소속을 변경할 수 있습니다." }
                require(members.findAllByOrganization_IdAndStatus(requireNotNull(oldGroup.id), ACTIVE).size == 1) {
                    "다른 구성원이 있는 개인 그룹 대표자는 운영자에게 대표자 변경을 요청해 주세요."
                }
                oldGroup.representativeAccount = null
                oldGroup.status = INACTIVE
            }
            it.status = LEFT
            members.saveAndFlush(it)
        }
        val existingTarget = members.findFirstByOrganization_IdAndAccount_Id(requireNotNull(target.id), accountId)
        if (existingTarget == null) {
            members.saveAndFlush(
                OrganizationMemberEntity().apply {
                    organization = target
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

    private fun lockGroup(id: Long): OrganizationEntity =
        groups.findLockedById(id)?.takeIf { it.status == ACTIVE }
            ?: throw ItemNotFoundException("활성 구매자 그룹을 찾을 수 없습니다.")

    private fun OrganizationEntity.toSummary(account: AccountEntity) =
        OrganizationSummaryDto(
            requireNotNull(publicId),
            organizationType,
            displayName,
            representativeAccount?.id == account.id,
            capabilities.findAllByOrganization_Id(requireNotNull(id)).mapTo(linkedSetOf()) { it.capabilityCode },
        )

    private fun OrganizationInvitationEntity.toModel() = OrganizationInvitationDto(
        requireNotNull(publicId),
        requireNotNull(organization.publicId),
        organization.displayName,
        phoneNormalized.takeLast(4).padStart(phoneNormalized.length, '*'),
    )

    private fun OrganizationJoinRequestEntity.toModel() = OrganizationJoinRequest(
        requireNotNull(publicId),
        requireNotNull(organization.publicId),
        organization.displayName,
        account.name,
        account.phoneNormalized?.let { "***-****-${it.takeLast(4)}" } ?: "",
        requestedAt,
    )

    private companion object {
        const val COMPLETED = "COMPLETED"
        const val ACTIVE = "ACTIVE"
        const val LEFT = "LEFT"
        const val PENDING = "PENDING"
        const val ACCEPTED = "ACCEPTED"
        const val DECLINED = "DECLINED"
        const val APPROVED = "APPROVED"
        const val INDIVIDUAL = "INDIVIDUAL"
        const val BUSINESS = "BUSINESS"
        const val INACTIVE = "INACTIVE"
        const val BUYER = "BUYER"
        const val SELLER = "SELLER"
    }
}