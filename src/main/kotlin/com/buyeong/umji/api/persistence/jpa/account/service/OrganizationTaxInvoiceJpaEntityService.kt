package com.buyeong.umji.api.persistence.jpa.account.service

import com.buyeong.umji.api.account.model.OrganizationTaxInvoiceProfile
import com.buyeong.umji.api.account.model.OrganizationTaxInvoiceProfileCommand
import com.buyeong.umji.api.order.model.TaxInvoiceSupplier
import com.buyeong.umji.api.exception.ForbiddenOperationException
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationBusinessProfileEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationBusinessProfileRepository
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationRepository
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrganizationTaxInvoiceJpaEntityService(
    private val organizations: OrganizationJpaEntityService,
    private val groups: OrganizationRepository,
    private val profiles: OrganizationBusinessProfileRepository,
) {
    fun forAccount(accountPublicId: UUID): OrganizationTaxInvoiceProfile? =
        organizations.activeBuyerForAccountPublicId(accountPublicId)?.toProfile()

    fun forCurrentAccount(accountPublicId: UUID): OrganizationTaxInvoiceProfile? =
        organizations.activeForAccountPublicId(accountPublicId)
            ?.takeIf { organization ->
                val id = requireNotNull(organization.publicId)
                organizations.hasCapability(id, BUYER) || organizations.hasCapability(id, SELLER)
            }?.toProfile()

    @Transactional
    fun updateForAccount(
        accountPublicId: UUID,
        command: OrganizationTaxInvoiceProfileCommand,
    ): OrganizationTaxInvoiceProfile? {
        val active = organizations.activeForAccountPublicId(accountPublicId) ?: return null
        val activePublicId = requireNotNull(active.publicId)
        require(organizations.hasCapability(activePublicId, BUYER) || organizations.hasCapability(activePublicId, SELLER)) {
            "구매 또는 판매 Organization이 필요합니다."
        }
        val group = groups.findLockedById(requireNotNull(active.id)) ?: return null
        if (group.organizationType != BUSINESS) throw IllegalArgumentException("사업자 그룹만 세금계산서 정보를 관리할 수 있습니다.")
        if (group.representativeAccount?.publicId != accountPublicId) {
            throw ForbiddenOperationException("그룹 대표자만 세금계산서 정보를 수정할 수 있습니다.")
        }
        val existing = profiles.findByOrganization_Id(requireNotNull(group.id))
        if (existing?.businessRegistrationVerificationStatus !in setOf("ACTIVE", "TEMPORARILY_CLOSED")) {
            throw IllegalStateException("사업자등록 상태 확인이 완료된 후 정보를 확인할 수 있습니다.")
        }
        if (
            existing?.businessRegistrationNumber != null &&
            command.businessRegistrationNumber.clean() != existing.businessRegistrationNumber.clean()
        ) {
            throw IllegalArgumentException("검증된 사업자등록번호는 변경할 수 없습니다.")
        }
        val profile = save(group, command)
        if (!profile.isComplete()) throw IllegalArgumentException("사업자등록 필수 정보를 모두 입력해야 합니다.")
        profile.businessRegistrationConfirmedAt = Instant.now()
        profiles.save(profile)
        return profile.toModel(group)
    }

    fun forGroup(groupPublicId: UUID): OrganizationTaxInvoiceProfile? =
        groups.findByPublicId(groupPublicId)?.takeIf { it.status == ACTIVE && hasBusinessCapability(groupPublicId) }?.toProfile()

    fun supplierForOrganization(organizationPublicId: UUID): TaxInvoiceSupplier? {
        val organization = groups.findByPublicId(organizationPublicId)
            ?.takeIf { it.status == ACTIVE && organizations.hasCapability(organizationPublicId, SELLER) }
            ?: return null
        val profile = profiles.findByOrganization_Id(requireNotNull(organization.id)) ?: return null
        val email = profile.taxInvoiceEmail?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (!profile.isComplete() || profile.businessRegistrationVerificationStatus !in setOf("ACTIVE", "TEMPORARILY_CLOSED") || profile.businessRegistrationConfirmedAt == null) return null
        return TaxInvoiceSupplier(
            requireNotNull(profile.businessRegistrationNumber),
            profile.businessName,
            requireNotNull(profile.representativeName),
            listOfNotNull(profile.postalCode, profile.address1, profile.address2?.takeIf(String::isNotBlank)).joinToString(" "),
            requireNotNull(profile.businessIndustry),
            requireNotNull(profile.businessItem),
            email,
        )
    }

    fun isSellerBusinessProfileReady(organizationPublicId: UUID): Boolean {
        val organization = groups.findByPublicId(organizationPublicId)
            ?.takeIf { it.status == ACTIVE && it.organizationType == BUSINESS && organizations.hasCapability(organizationPublicId, SELLER) }
            ?: return false
        val profile = profiles.findByOrganization_Id(requireNotNull(organization.id)) ?: return false
        return profile.isComplete() && profile.businessRegistrationVerificationStatus == "ACTIVE" && profile.businessRegistrationConfirmedAt != null
    }

    @Transactional
    fun updateForGroup(
        groupPublicId: UUID,
        command: OrganizationTaxInvoiceProfileCommand,
    ): OrganizationTaxInvoiceProfile? {
        val group = groups.findByPublicId(groupPublicId)?.takeIf { it.status == ACTIVE && hasBusinessCapability(groupPublicId) } ?: return null
        val lockedGroup = groups.findLockedById(requireNotNull(group.id))?.takeIf { it.status == ACTIVE } ?: return null
        if (lockedGroup.organizationType != BUSINESS) throw IllegalArgumentException("사업자 그룹만 세금계산서 정보를 관리할 수 있습니다.")
        return save(lockedGroup, command).toModel(lockedGroup)
    }

    private fun save(group: OrganizationEntity, command: OrganizationTaxInvoiceProfileCommand): OrganizationBusinessProfileEntity {
        val organizationId = requireNotNull(group.id)
        val profile = profiles.findByOrganization_Id(organizationId) ?: OrganizationBusinessProfileEntity().apply {
            organization = group
            businessName = group.displayName
            status = COMPLETED
        }
        val registrationNumberChanged = profile.businessRegistrationNumber != command.businessRegistrationNumber.clean()
        profile.businessRegistrationNumber = command.businessRegistrationNumber.clean()
        if (registrationNumberChanged) {
            profile.businessRegistrationVerificationStatus = if (command.businessRegistrationNumber.clean() == null) "NOT_REQUIRED" else "PENDING"
            profile.businessRegistrationVerifiedAt = null
            profile.businessRegistrationConfirmedAt = null
        }
        profile.businessName = command.businessName.trim()
        profile.representativeName = command.representativeName.clean()
        profile.postalCode = command.postalCode.clean()
        profile.address1 = command.address1.clean()
        profile.address2 = command.address2.clean()
        profile.businessIndustry = command.businessIndustry.clean()
        profile.businessItem = command.businessItem.clean()
        profile.taxInvoiceEmail = command.email.clean()
        profile.updatedAt = Instant.now()
        profiles.save(profile)
        return profile
    }

    private fun OrganizationEntity.toProfile(): OrganizationTaxInvoiceProfile {
        val profile = id?.let(profiles::findByOrganization_Id)
        val complete = organizationType == BUSINESS
            && profile.isComplete()
            && profile?.businessRegistrationVerificationStatus in setOf("ACTIVE", "TEMPORARILY_CLOSED")
            && profile?.businessRegistrationConfirmedAt != null
        return OrganizationTaxInvoiceProfile(
            organizationId = requireNotNull(publicId),
            organizationType = organizationType,
            businessRegistrationNumber = profile?.businessRegistrationNumber,
            businessName = profile?.businessName,
            representativeName = profile?.representativeName,
            postalCode = profile?.postalCode,
            address1 = profile?.address1,
            address2 = profile?.address2,
            businessIndustry = profile?.businessIndustry,
            businessItem = profile?.businessItem,
            email = profile?.taxInvoiceEmail,
            complete = complete,
            businessRegistrationVerificationStatus = profile?.businessRegistrationVerificationStatus ?: "NOT_REQUIRED",
            businessRegistrationVerifiedAt = profile?.businessRegistrationVerifiedAt,
            businessRegistrationConfirmedAt = profile?.businessRegistrationConfirmedAt,
        )
    }

    private fun OrganizationBusinessProfileEntity?.isComplete(): Boolean = this != null && listOf(
        businessRegistrationNumber,
        businessName,
        representativeName,
        postalCode,
        address1,
        businessIndustry,
        businessItem,
    ).all { !it.isNullOrBlank() }

    private fun OrganizationBusinessProfileEntity.toModel(group: OrganizationEntity) = OrganizationTaxInvoiceProfile(
        organizationId = requireNotNull(group.publicId),
        organizationType = group.organizationType,
        businessRegistrationNumber = businessRegistrationNumber,
        businessName = businessName,
        representativeName = representativeName,
        postalCode = postalCode,
        address1 = address1,
        address2 = address2,
        businessIndustry = businessIndustry,
        businessItem = businessItem,
        email = taxInvoiceEmail,
        complete = group.organizationType == BUSINESS
            && isComplete()
            && businessRegistrationVerificationStatus in setOf("ACTIVE", "TEMPORARILY_CLOSED")
            && businessRegistrationConfirmedAt != null,
        businessRegistrationVerificationStatus = businessRegistrationVerificationStatus,
        businessRegistrationVerifiedAt = businessRegistrationVerifiedAt,
        businessRegistrationConfirmedAt = businessRegistrationConfirmedAt,
    )

    private fun String?.clean() = this?.trim()?.ifBlank { null }

    private fun hasBusinessCapability(organizationPublicId: UUID) =
        organizations.hasCapability(organizationPublicId, BUYER) || organizations.hasCapability(organizationPublicId, SELLER)

    private companion object {
        const val ACTIVE = "ACTIVE"
        const val BUSINESS = "BUSINESS"
        const val COMPLETED = "COMPLETED"
        const val BUYER = "BUYER"
        const val SELLER = "SELLER"
    }
}
