package com.buyeong.umji.api.persistence.jpa.account

import com.buyeong.umji.api.account.model.BuyerGroupTaxInvoiceProfile
import com.buyeong.umji.api.account.model.BuyerGroupTaxInvoiceProfileCommand
import com.buyeong.umji.api.exception.ForbiddenOperationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class BuyerGroupTaxInvoiceJpaEntityService(
    private val buyerGroups: BuyerGroupJpaEntityService,
    private val groups: BuyerGroupRepository,
    private val profiles: BuyerGroupBusinessProfileRepository,
) {
    fun forAccount(accountPublicId: UUID): BuyerGroupTaxInvoiceProfile? =
        buyerGroups.activeForAccountPublicId(accountPublicId)?.toProfile()

    @Transactional
    fun updateForAccount(
        accountPublicId: UUID,
        command: BuyerGroupTaxInvoiceProfileCommand,
    ): BuyerGroupTaxInvoiceProfile? {
        val group = buyerGroups.lockActiveForAccountPublicId(accountPublicId)
        if (group.groupType != BUSINESS) throw IllegalArgumentException("사업자 그룹만 세금계산서 정보를 관리할 수 있습니다.")
        if (group.representativeAccount?.publicId != accountPublicId) {
            throw ForbiddenOperationException("그룹 대표자만 세금계산서 정보를 수정할 수 있습니다.")
        }
        val existing = profiles.findByBuyerGroup_Id(requireNotNull(group.id))
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

    fun forGroup(groupPublicId: UUID): BuyerGroupTaxInvoiceProfile? =
        groups.findByPublicId(groupPublicId)?.takeIf { it.status == ACTIVE }?.toProfile()

    @Transactional
    fun updateForGroup(
        groupPublicId: UUID,
        command: BuyerGroupTaxInvoiceProfileCommand,
    ): BuyerGroupTaxInvoiceProfile? {
        val group = groups.findByPublicId(groupPublicId)?.takeIf { it.status == ACTIVE } ?: return null
        val lockedGroup = groups.findLockedById(requireNotNull(group.id))?.takeIf { it.status == ACTIVE } ?: return null
        if (lockedGroup.groupType != BUSINESS) throw IllegalArgumentException("사업자 그룹만 세금계산서 정보를 관리할 수 있습니다.")
        return save(lockedGroup, command).toModel(lockedGroup)
    }

    private fun save(group: BuyerGroupEntity, command: BuyerGroupTaxInvoiceProfileCommand): BuyerGroupBusinessProfileEntity {
        val groupId = requireNotNull(group.id)
        val profile = profiles.findByBuyerGroup_Id(groupId) ?: BuyerGroupBusinessProfileEntity().apply {
            buyerGroup = group
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

    private fun BuyerGroupEntity.toProfile(): BuyerGroupTaxInvoiceProfile {
        val profile = id?.let(profiles::findByBuyerGroup_Id)
        val complete = groupType == BUSINESS
            && profile.isComplete()
            && profile?.businessRegistrationVerificationStatus in setOf("ACTIVE", "TEMPORARILY_CLOSED")
            && profile?.businessRegistrationConfirmedAt != null
        return BuyerGroupTaxInvoiceProfile(
            buyerGroupId = requireNotNull(publicId),
            groupType = groupType,
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

    private fun BuyerGroupBusinessProfileEntity?.isComplete(): Boolean = this != null && listOf(
        businessRegistrationNumber,
        businessName,
        representativeName,
        postalCode,
        address1,
        businessIndustry,
        businessItem,
    ).all { !it.isNullOrBlank() }

    private fun BuyerGroupBusinessProfileEntity.toModel(group: BuyerGroupEntity) = BuyerGroupTaxInvoiceProfile(
        buyerGroupId = requireNotNull(group.publicId),
        groupType = group.groupType,
        businessRegistrationNumber = businessRegistrationNumber,
        businessName = businessName,
        representativeName = representativeName,
        postalCode = postalCode,
        address1 = address1,
        address2 = address2,
        businessIndustry = businessIndustry,
        businessItem = businessItem,
        email = taxInvoiceEmail,
        complete = group.groupType == BUSINESS
            && isComplete()
            && businessRegistrationVerificationStatus in setOf("ACTIVE", "TEMPORARILY_CLOSED")
            && businessRegistrationConfirmedAt != null,
        businessRegistrationVerificationStatus = businessRegistrationVerificationStatus,
        businessRegistrationVerifiedAt = businessRegistrationVerifiedAt,
        businessRegistrationConfirmedAt = businessRegistrationConfirmedAt,
    )

    private fun String?.clean() = this?.trim()?.ifBlank { null }

    private companion object {
        const val ACTIVE = "ACTIVE"
        const val BUSINESS = "BUSINESS"
        const val COMPLETED = "COMPLETED"
    }
}
