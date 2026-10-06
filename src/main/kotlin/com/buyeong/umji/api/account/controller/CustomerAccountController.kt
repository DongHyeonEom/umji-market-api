package com.buyeong.umji.api.account.controller

import com.buyeong.umji.api.account.model.OrganizationOnboardingResponse
import com.buyeong.umji.api.account.model.OrganizationRegistrationRequest
import com.buyeong.umji.api.account.model.OrganizationTaxInvoiceProfileRequest
import com.buyeong.umji.api.account.model.CreateIndividualGroupRequest
import com.buyeong.umji.api.account.model.CustomerProfileResponse
import com.buyeong.umji.api.account.model.DecideOrganizationJoinRequest
import com.buyeong.umji.api.account.model.InviteOrganizationMemberRequest
import com.buyeong.umji.api.account.model.RequestOrganizationJoinRequest
import com.buyeong.umji.api.account.model.SharedAddressRequest
import com.buyeong.umji.api.account.model.SharedAddressResponse
import com.buyeong.umji.api.account.model.toCommand
import com.buyeong.umji.api.account.model.toResponse
import com.buyeong.umji.api.account.model.toResponse as membershipResponse
import com.buyeong.umji.api.account.model.toResponse as taxInvoiceProfileResponse
import com.buyeong.umji.api.account.service.OrganizationMembershipService
import com.buyeong.umji.api.account.service.OrganizationTaxInvoiceProfileService
import com.buyeong.umji.api.account.service.CustomerAccountService
import com.buyeong.umji.api.auth.service.CurrentAccountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/account")
@Validated
@Tag(name = "사용자 계정", description = "사용자 프로필·배송지·Organization 가입 API")
class CustomerAccountController(
    private val currentAccounts: CurrentAccountService,
    private val account: CustomerAccountService,
    private val groups: OrganizationMembershipService,
    private val taxInvoiceProfiles: OrganizationTaxInvoiceProfileService,
) {
    @Operation(summary = "사용자 프로필조회", description = "사용자 프로필·배송지·Organization 가입 API. /profile 경로에서 사용자 프로필조회를 수행")
    @GetMapping("/profile")
    fun profile(): CustomerProfileResponse = account.profile(currentAccounts.activeAccountPublicId()).toResponse()

    @Operation(summary = "배송지조회", description = "사용자 프로필·배송지·Organization 가입 API. /addresses 경로에서 배송지조회를 수행")
    @GetMapping("/addresses")
    fun addresses(): List<SharedAddressResponse> = account.addresses(currentAccounts.activeAccountPublicId()).map { it.toResponse() }

    @Operation(summary = "배송지등록", description = "사용자 프로필·배송지·Organization 가입 API. /addresses 경로에서 배송지등록를 수행")
    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    fun createAddress(@Valid @RequestBody request: SharedAddressRequest): SharedAddressResponse =
        account.createAddress(currentAccounts.activeAccountPublicId(), request.toCommand()).toResponse()

    @Operation(summary = "배송지수정", description = "사용자 프로필·배송지·Organization 가입 API. /addresses/{addressId} 경로에서 배송지수정를 수행")
    @PutMapping("/addresses/{addressId}")
    fun updateAddress(
        @Parameter(description = "대상 배송지 공개 식별자(UUID)") @PathVariable addressId: UUID,
        @Valid @RequestBody request: SharedAddressRequest,
    ): SharedAddressResponse = account.updateAddress(currentAccounts.activeAccountPublicId(), addressId, request.toCommand()).toResponse()

    @Operation(summary = "배송지수정", description = "사용자 프로필·배송지·Organization 가입 API. /addresses/{addressId}/default 경로에서 배송지수정를 수행")
    @PutMapping("/addresses/{addressId}/default")
    fun setDefaultAddress(@Parameter(description = "대상 배송지 공개 식별자(UUID)") @PathVariable addressId: UUID): SharedAddressResponse =
        account.setDefaultAddress(currentAccounts.activeAccountPublicId(), addressId).toResponse()

    @Operation(summary = "배송지삭제", description = "사용자 프로필·배송지·Organization 가입 API. /addresses/{addressId} 경로에서 배송지삭제를 수행")
    @DeleteMapping("/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAddress(@Parameter(description = "대상 배송지 공개 식별자(UUID)") @PathVariable addressId: UUID) = account.deleteAddress(currentAccounts.activeAccountPublicId(), addressId)

    @Operation(summary = "Organization조회", description = "사용자 프로필·배송지·Organization 가입 API. /organizations/current 경로에서 Organization조회를 수행")
    @GetMapping("/organizations/current")
    fun currentOrganization() = groups.current(currentAccounts.activeAccountPublicId())?.toResponse()

    @Operation(summary = "그룹 세금계산서 정보 조회", description = "현재 활성 Organization의 공급받는자 세금계산서 정보를 조회")
    @GetMapping("/organizations/current/tax-invoice-profile")
    fun taxInvoiceProfile() = taxInvoiceProfiles.forAccount(currentAccounts.activeAccountPublicId()).taxInvoiceProfileResponse()

    @Operation(summary = "그룹 세금계산서 정보 수정", description = "현재 활성 Organization의 공급받는자 세금계산서 정보를 대표자가 수정")
    @PutMapping("/organizations/current/tax-invoice-profile")
    fun updateTaxInvoiceProfile(@Valid @RequestBody request: OrganizationTaxInvoiceProfileRequest) =
        taxInvoiceProfiles.updateForAccount(currentAccounts.activeAccountPublicId(), request.toCommand()).taxInvoiceProfileResponse()

    @Operation(summary = "Organization조회", description = "사용자 프로필·배송지·Organization 가입 API. /organizations/onboarding 경로에서 Organization조회를 수행")
    @GetMapping("/organizations/onboarding")
    fun groupOnboarding(): OrganizationOnboardingResponse {
        val accountId = currentAccounts.activeAccountPublicId()
        return OrganizationOnboardingResponse(
            groups.current(accountId)?.toResponse(),
            groups.invitations(accountId).map { it.toResponse() },
        )
    }

    @Operation(summary = "Organization등록", description = "사용자 프로필·배송지·Organization 가입 API. /organizations/individual 경로에서 Organization등록를 수행")
    @PostMapping("/organizations/individual")
    @ResponseStatus(HttpStatus.CREATED)
    fun createIndividualGroup(@Valid @RequestBody request: CreateIndividualGroupRequest) =
        groups.createIndividualGroup(currentAccounts.activeAccountPublicId(), request.name).toResponse()

    @Operation(summary = "Organization 유형 등록", description = "미소속 사용자의 개인 또는 사업자 그룹 등록. 사업자 그룹은 사업자등록 상태 확인 후 등록")
    @PostMapping("/organizations")
    @ResponseStatus(HttpStatus.CREATED)
    fun registerGroup(@Valid @RequestBody request: OrganizationRegistrationRequest) =
        groups.register(currentAccounts.activeAccountPublicId(), request.toCommand()).toResponse()

    @Operation(summary = "Organization조회", description = "사용자 프로필·배송지·Organization 가입 API. /organizations/search 경로에서 Organization조회를 수행")
    @GetMapping("/organizations/search")
    fun searchGroups(
        @Parameter(description = "대표자 휴대폰 번호") @RequestParam @NotBlank phone: String,
        @Parameter(description = "검색할 Organization capability") @RequestParam(defaultValue = "BUYER") capability: String,
    ) = groups.search(phone, capability).map { it.membershipResponse() }

    @Operation(summary = "Organization등록", description = "사용자 프로필·배송지·Organization 가입 API. /organizations/invitations 경로에서 Organization등록를 수행")
    @PostMapping("/organizations/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    fun invite(@Valid @RequestBody request: InviteOrganizationMemberRequest) =
        groups.invite(currentAccounts.activeAccountPublicId(), request.phone).toResponse()

    @Operation(summary = "Organization조회", description = "사용자 프로필·배송지·Organization 가입 API. /organizations/invitations 경로에서 Organization조회를 수행")
    @GetMapping("/organizations/invitations")
    fun invitations() = groups.invitations(currentAccounts.activeAccountPublicId()).map { it.toResponse() }

    @Operation(summary = "Organization등록", description = "사용자 프로필·배송지·Organization 가입 API. /organizations/invitations/{invitationId}/response 경로에서 Organization등록를 수행")
    @PostMapping("/organizations/invitations/{invitationId}/response")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun respondInvitation(
        @Parameter(description = "그룹 초대 공개 식별자(UUID)") @PathVariable invitationId: UUID,
        @Valid @RequestBody request: DecideOrganizationJoinRequest,
    ) = groups.respondInvitation(currentAccounts.activeAccountPublicId(), invitationId, request.approved)

    @Operation(summary = "Organization등록", description = "사용자 프로필·배송지·Organization 가입 API. /organizations/join-requests 경로에서 Organization등록를 수행")
    @PostMapping("/organizations/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    fun requestToJoin(@Valid @RequestBody request: RequestOrganizationJoinRequest) =
        groups.requestToJoin(currentAccounts.activeAccountPublicId(), request.organizationId)

    @Operation(summary = "Organization조회", description = "사용자 프로필·배송지·Organization 가입 API. /organizations/join-requests 경로에서 Organization조회를 수행")
    @GetMapping("/organizations/join-requests")
    fun pendingJoinRequests() = groups.pendingJoinRequests(currentAccounts.activeAccountPublicId()).map { it.toResponse() }

    @Operation(summary = "Organization등록", description = "사용자 프로필·배송지·Organization 가입 API. /organizations/join-requests/{requestId}/response 경로에서 Organization등록를 수행")
    @PostMapping("/organizations/join-requests/{requestId}/response")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun respondJoinRequest(
        @Parameter(description = "가입 요청 공개 식별자(UUID)") @PathVariable requestId: UUID,
        @Valid @RequestBody request: DecideOrganizationJoinRequest,
    ) = groups.respondJoinRequest(currentAccounts.activeAccountPublicId(), requestId, request.approved)
}
