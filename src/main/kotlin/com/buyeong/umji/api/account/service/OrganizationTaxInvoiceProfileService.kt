package com.buyeong.umji.api.account.service

import com.buyeong.umji.api.account.model.OrganizationTaxInvoiceProfile
import com.buyeong.umji.api.account.model.OrganizationTaxInvoiceProfileCommand
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationTaxInvoiceJpaEntityService
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OrganizationTaxInvoiceProfileService(
    private val profiles: OrganizationTaxInvoiceJpaEntityService,
) {
    @Transactional(readOnly = true)
    fun forAccount(accountPublicId: UUID): OrganizationTaxInvoiceProfile =
        profiles.forAccount(accountPublicId) ?: throw ItemNotFoundException("활성 Organization을 찾을 수 없습니다.")

    @Transactional
    fun updateForAccount(
        accountPublicId: UUID,
        command: OrganizationTaxInvoiceProfileCommand,
    ): OrganizationTaxInvoiceProfile {
        validate(command)
        return profiles.updateForAccount(accountPublicId, command)
            ?: throw ItemNotFoundException("활성 사업자 그룹을 찾을 수 없습니다.")
    }

    @Transactional(readOnly = true)
    fun forGroup(groupPublicId: UUID): OrganizationTaxInvoiceProfile =
        profiles.forGroup(groupPublicId) ?: throw ItemNotFoundException("Organization을 찾을 수 없습니다.")

    @Transactional
    fun updateForGroup(
        groupPublicId: UUID,
        command: OrganizationTaxInvoiceProfileCommand,
    ): OrganizationTaxInvoiceProfile {
        validate(command)
        return profiles.updateForGroup(groupPublicId, command)
            ?: throw ItemNotFoundException("활성 사업자 그룹을 찾을 수 없습니다.")
    }

    private fun validate(command: OrganizationTaxInvoiceProfileCommand) {
        require(command.businessName.isNotBlank() && command.businessName.length <= 200) { "상호는 1~200자여야 합니다." }
        require(command.businessRegistrationNumber == null || command.businessRegistrationNumber.length <= 30) { "사업자등록번호는 30자 이하여야 합니다." }
        require(command.representativeName == null || command.representativeName.length <= 100) { "성명은 100자 이하여야 합니다." }
        require(command.postalCode == null || command.postalCode.length <= 20) { "우편번호는 20자 이하여야 합니다." }
        require(command.address1 == null || command.address1.length <= 255) { "사업자주소는 255자 이하여야 합니다." }
        require(command.address2 == null || command.address2.length <= 255) { "상세주소는 255자 이하여야 합니다." }
        require(command.businessIndustry == null || command.businessIndustry.length <= 100) { "업태는 100자 이하여야 합니다." }
        require(command.businessItem == null || command.businessItem.length <= 100) { "종목은 100자 이하여야 합니다." }
        require(command.email == null || command.email.length <= 255) { "이메일은 255자 이하여야 합니다." }
        require(command.email == null || EMAIL_PATTERN.matches(command.email)) { "이메일 형식이 올바르지 않습니다." }
    }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
