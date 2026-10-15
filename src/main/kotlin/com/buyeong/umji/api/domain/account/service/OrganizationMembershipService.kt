package com.buyeong.umji.api.domain.account.service

import com.buyeong.umji.api.domain.account.dto.OrganizationInvitationDto
import com.buyeong.umji.api.domain.account.dto.OrganizationRegistrationCommandDto
import com.buyeong.umji.api.domain.account.dto.OrganizationSearchResultDto
import com.buyeong.umji.api.domain.account.dto.OrganizationSummaryDto
import com.buyeong.umji.api.domain.account.integration.http.BusinessRegistrationStatusClient
import com.buyeong.umji.api.domain.account.model.OrganizationJoinRequest
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationMembershipJpaEntityService
import com.buyeong.umji.api.util.PhoneNumberHelper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OrganizationMembershipService(
    private val groups: OrganizationMembershipJpaEntityService,
    private val registrationStatus: BusinessRegistrationStatusClient,
) {
    @Transactional(readOnly = true)
    fun current(accountId: UUID): OrganizationSummaryDto? = groups.current(accountId)

    @Transactional
    fun createIndividualGroup(accountId: UUID, name: String): OrganizationSummaryDto {
        return groups.createIndividualGroup(accountId, name.trim())
    }

    fun register(accountId: UUID, command: OrganizationRegistrationCommandDto): OrganizationSummaryDto {
        require(groups.current(accountId) == null) { "이미 활성 Organization에 소속되어 있습니다." }
        if (command.type == INDIVIDUAL) {
            require(command.business == null) { "개인 그룹 등록에는 사업자등록 정보를 입력할 수 없습니다." }
            return groups.register(accountId, command)
        }

        val business = requireNotNull(command.business) { "사업자 그룹 등록 정보가 필요합니다." }
        require(business.confirmed) { "사업자등록 내용을 확인해 주세요." }
        registrationStatus.ensureNotClosed(business.businessRegistrationNumber.replace("-", ""))
        return groups.register(
            accountId,
            command.copy(type = BUSINESS, business = business.copy(businessRegistrationNumber = business.businessRegistrationNumber.replace("-", "")))
        )
    }

    @Transactional(readOnly = true)
    fun search(phone: String, capability: String = "BUYER"): List<OrganizationSearchResultDto> {
        return groups.search(PhoneNumberHelper.normalizeMobilePhoneNumber(phone), capability)
    }

    @Transactional
    fun invite(accountId: UUID, phone: String): OrganizationInvitationDto =
        groups.invite(accountId, PhoneNumberHelper.normalizeMobilePhoneNumber(phone))

    @Transactional(readOnly = true)
    fun invitations(accountId: UUID): List<OrganizationInvitationDto> = groups.invitations(accountId)

    @Transactional
    fun respondInvitation(accountId: UUID, invitationId: UUID, accept: Boolean) =
        groups.respondInvitation(accountId, invitationId, accept)

    @Transactional
    fun requestToJoin(accountId: UUID, organizationId: UUID) = groups.requestToJoin(accountId, organizationId)

    @Transactional(readOnly = true)
    fun pendingJoinRequests(accountId: UUID): List<OrganizationJoinRequest> = groups.pendingJoinRequests(accountId)

    @Transactional
    fun respondJoinRequest(accountId: UUID, requestId: UUID, approve: Boolean) =
        groups.respondJoinRequest(accountId, requestId, approve)

    private companion object {
        const val INDIVIDUAL = "INDIVIDUAL"
        const val BUSINESS = "BUSINESS"
    }
}
