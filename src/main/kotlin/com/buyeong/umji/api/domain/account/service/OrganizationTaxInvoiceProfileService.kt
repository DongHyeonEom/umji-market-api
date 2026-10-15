package com.buyeong.umji.api.domain.account.service

import com.buyeong.umji.api.domain.account.dto.OrganizationTaxInvoiceProfileCommandDto
import com.buyeong.umji.api.domain.account.dto.OrganizationTaxInvoiceProfileDto
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationTaxInvoiceJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrganizationTaxInvoiceProfileService(
    private val profiles: OrganizationTaxInvoiceJpaEntityService,
) {
    @Transactional(readOnly = true)
    fun forAccount(accountPublicId: UUID): OrganizationTaxInvoiceProfileDto =
        profiles.forCurrentAccount(accountPublicId) ?: throw ItemNotFoundException("활성 Organization을 찾을 수 없습니다.")

    @Transactional
    fun updateForAccount(
        accountPublicId: UUID,
        command: OrganizationTaxInvoiceProfileCommandDto,
    ): OrganizationTaxInvoiceProfileDto {
        return profiles.updateForAccount(accountPublicId, command)
            ?: throw ItemNotFoundException("활성 사업자 그룹을 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun forGroup(groupPublicId: UUID): OrganizationTaxInvoiceProfileDto =
        profiles.forGroup(groupPublicId) ?: throw ItemNotFoundException("Organization을 찾을 수 없습니다.")

    @Transactional(readOnly = true)
    fun isSellerBusinessProfileReady(organizationId: UUID): Boolean = profiles.isSellerBusinessProfileReady(organizationId)

    @Transactional
    fun updateForGroup(
        groupPublicId: UUID,
        command: OrganizationTaxInvoiceProfileCommandDto,
    ): OrganizationTaxInvoiceProfileDto {
        return profiles.updateForGroup(groupPublicId, command)
            ?: throw ItemNotFoundException("활성 사업자 그룹을 찾을 수 없습니다.")
    }

}
