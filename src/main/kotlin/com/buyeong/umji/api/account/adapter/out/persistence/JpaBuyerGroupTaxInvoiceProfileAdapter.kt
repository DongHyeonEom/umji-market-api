package com.buyeong.umji.api.account.adapter.out.persistence

import com.buyeong.umji.api.account.application.model.BuyerGroupTaxInvoiceProfile
import com.buyeong.umji.api.account.application.model.BuyerGroupTaxInvoiceProfileCommand
import com.buyeong.umji.api.account.application.port.out.BuyerGroupTaxInvoiceProfilePort
import com.buyeong.umji.api.exception.ForbiddenOperationException
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupBusinessProfileEntity
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupBusinessProfileRepository
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupEntity
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class JpaBuyerGroupTaxInvoiceProfileAdapter(
    private val buyerGroups: BuyerGroupJpaEntityService,
    private val groups: BuyerGroupRepository,
    private val profiles: BuyerGroupBusinessProfileRepository,
) : BuyerGroupTaxInvoiceProfilePort {
    @Transactional(readOnly = true)
    override fun forAccount(accountPublicId: UUID): BuyerGroupTaxInvoiceProfile? =
        buyerGroups.activeForAccountPublicId(accountPublicId)?.toProfile()

    @Transactional
    override fun updateForAccount(
        accountPublicId: UUID,
        command: BuyerGroupTaxInvoiceProfileCommand,
    ): BuyerGroupTaxInvoiceProfile? {
        val group = buyerGroups.lockActiveForAccountPublicId(accountPublicId)
        if (group.groupType != BUSINESS) throw IllegalArgumentException("사업자 그룹만 세금계산서 정보를 관리할 수 있습니다.")
        if (group.representativeAccount?.publicId != accountPublicId) {
            throw ForbiddenOperationException("그룹 대표자만 세금계산서 정보를 수정할 수 있습니다.")
        }
        return save(group, command)
    }

    @Transactional(readOnly = true)
    override fun forGroup(groupPublicId: UUID): BuyerGroupTaxInvoiceProfile? =
        groups.findByPublicId(groupPublicId)?.takeIf { it.status == ACTIVE }?.toProfile()

    @Transactional
    override fun updateForGroup(
        groupPublicId: UUID,
        command: BuyerGroupTaxInvoiceProfileCommand,
    ): BuyerGroupTaxInvoiceProfile? {
        val group = groups.findByPublicId(groupPublicId)?.takeIf { it.status == ACTIVE } ?: return null
        val lockedGroup = groups.findLockedById(requireNotNull(group.id))?.takeIf { it.status == ACTIVE } ?: return null
        if (lockedGroup.groupType != BUSINESS) throw IllegalArgumentException("사업자 그룹만 세금계산서 정보를 관리할 수 있습니다.")
        return save(lockedGroup, command)
    }

    private fun save(group: BuyerGroupEntity, command: BuyerGroupTaxInvoiceProfileCommand): BuyerGroupTaxInvoiceProfile {
        val groupId = requireNotNull(group.id)
        val profile = profiles.findByBuyerGroup_Id(groupId) ?: BuyerGroupBusinessProfileEntity().apply {
            buyerGroup = group
            businessName = group.displayName
            status = COMPLETED
        }
        profile.businessRegistrationNumber = command.businessRegistrationNumber.clean()
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
        return profile.toModel(group)
    }

    private fun BuyerGroupEntity.toProfile(): BuyerGroupTaxInvoiceProfile {
        val profile = id?.let(profiles::findByBuyerGroup_Id)
        val complete = groupType == BUSINESS && profile.isComplete()
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
        complete = group.groupType == BUSINESS && isComplete(),
    )

    private fun String?.clean() = this?.trim()?.ifBlank { null }

    private companion object {
        const val ACTIVE = "ACTIVE"
        const val BUSINESS = "BUSINESS"
        const val COMPLETED = "COMPLETED"
    }
}
