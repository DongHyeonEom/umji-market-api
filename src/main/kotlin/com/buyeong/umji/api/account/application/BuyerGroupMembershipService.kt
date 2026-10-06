package com.buyeong.umji.api.account.application

import com.buyeong.umji.api.account.application.model.BuyerGroupInvitation
import com.buyeong.umji.api.account.application.model.BuyerGroupJoinRequest
import com.buyeong.umji.api.account.application.model.BuyerGroupSearchResult
import com.buyeong.umji.api.account.application.model.BuyerGroupSummary
import com.buyeong.umji.api.account.application.model.BuyerGroupRegistrationCommand
import com.buyeong.umji.api.account.application.port.`in`.BuyerGroupMembershipUseCase
import com.buyeong.umji.api.account.application.port.out.BusinessRegistrationStatusPort
import com.buyeong.umji.api.account.application.port.out.BuyerGroupMembershipPort
import com.buyeong.umji.api.util.PhoneNumberHelper
import java.util.UUID

class BuyerGroupMembershipService(
    private val groups: BuyerGroupMembershipPort,
    private val registrationStatus: BusinessRegistrationStatusPort,
) : BuyerGroupMembershipUseCase {
    override fun current(accountId: UUID): BuyerGroupSummary? = groups.current(accountId)

    override fun createIndividualGroup(accountId: UUID, name: String): BuyerGroupSummary {
        require(name.isNotBlank() && name.length <= 200) { "그룹 이름은 1자 이상 200자 이하여야 합니다." }
        return groups.createIndividualGroup(accountId, name.trim())
    }

    override fun register(accountId: UUID, command: BuyerGroupRegistrationCommand): BuyerGroupSummary {
        require(groups.current(accountId) == null) { "이미 활성 구매자 그룹에 소속되어 있습니다." }
        require(command.type == INDIVIDUAL || command.type == BUSINESS) { "그룹 유형은 INDIVIDUAL 또는 BUSINESS여야 합니다." }
        if (command.type == INDIVIDUAL) {
            require(command.business == null) { "개인 그룹 등록에는 사업자등록 정보를 입력할 수 없습니다." }
            return groups.register(accountId, command)
        }

        val business = requireNotNull(command.business) { "사업자 그룹 등록 정보가 필요합니다." }
        require(business.confirmed) { "사업자등록 내용을 확인해 주세요." }
        require(business.businessRegistrationNumber.replace("-", "").matches(Regex("\\d{10}"))) { "사업자등록번호는 숫자 10자리여야 합니다." }
        require(business.businessName.isNotBlank() && business.businessName.length <= 200) { "상호는 1자 이상 200자 이하여야 합니다." }
        require(business.representativeName.isNotBlank() && business.representativeName.length <= 100) { "대표자 성명은 1자 이상 100자 이하여야 합니다." }
        require(business.postalCode.isNotBlank() && business.postalCode.length <= 20) { "사업자등록 우편번호를 입력해 주세요." }
        require(business.address1.isNotBlank() && business.address1.length <= 255) { "사업자등록 주소를 입력해 주세요." }
        require(business.address2.isNullOrBlank() || business.address2.length <= 255) { "사업자등록 상세 주소는 255자 이하여야 합니다." }
        require(business.businessIndustry.isNotBlank() && business.businessIndustry.length <= 100) { "업태를 입력해 주세요." }
        require(business.businessItem.isNotBlank() && business.businessItem.length <= 100) { "종목을 입력해 주세요." }

        registrationStatus.ensureNotClosed(business.businessRegistrationNumber.replace("-", ""))
        return groups.register(accountId, command.copy(type = BUSINESS, business = business.copy(businessRegistrationNumber = business.businessRegistrationNumber.replace("-", ""))))
    }

    override fun search(phone: String): List<BuyerGroupSearchResult> =
        groups.search(PhoneNumberHelper.normalizeMobilePhoneNumber(phone))

    override fun invite(accountId: UUID, phone: String): BuyerGroupInvitation =
        groups.invite(accountId, PhoneNumberHelper.normalizeMobilePhoneNumber(phone))

    override fun invitations(accountId: UUID): List<BuyerGroupInvitation> = groups.invitations(accountId)

    override fun respondInvitation(accountId: UUID, invitationId: UUID, accept: Boolean) =
        groups.respondInvitation(accountId, invitationId, accept)

    override fun requestToJoin(accountId: UUID, groupId: UUID) = groups.requestToJoin(accountId, groupId)

    override fun pendingJoinRequests(accountId: UUID): List<BuyerGroupJoinRequest> = groups.pendingJoinRequests(accountId)

    override fun respondJoinRequest(accountId: UUID, requestId: UUID, approve: Boolean) =
        groups.respondJoinRequest(accountId, requestId, approve)

    private companion object {
        const val INDIVIDUAL = "INDIVIDUAL"
        const val BUSINESS = "BUSINESS"
    }
}
