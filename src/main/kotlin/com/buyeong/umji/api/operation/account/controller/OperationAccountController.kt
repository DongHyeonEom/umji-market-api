package com.buyeong.umji.api.operation.account.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.auth.service.WebAuthenticationService
import com.buyeong.umji.api.operation.account.model.AccountData
import com.buyeong.umji.api.operation.account.model.BusinessProfileData
import com.buyeong.umji.api.operation.account.model.ConsentCommand
import com.buyeong.umji.api.operation.account.model.ManagedRole
import com.buyeong.umji.api.operation.account.model.NewAccount
import com.buyeong.umji.api.operation.account.service.OperationAccountService
import com.buyeong.umji.api.operation.model.AssignBuyerGroupRequest
import com.buyeong.umji.api.operation.model.BusinessProfileRequest
import com.buyeong.umji.api.operation.model.CreateConsentRequest
import com.buyeong.umji.api.operation.model.CreateOperationAccountRequest
import com.buyeong.umji.api.operation.model.OperationAccountResponse
import com.buyeong.umji.api.operation.model.OperationBusinessProfileResponse
import com.buyeong.umji.api.operation.model.OperationConsentResponse
import com.buyeong.umji.api.operation.model.OperationRoleResponse
import com.buyeong.umji.api.operation.model.UpdateAccountStatusRequest
import com.buyeong.umji.api.util.PhoneNumberHelper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/operation/accounts")
@Validated
@Tag(name = "운영 계정 관리", description = "운영자 계정·상태·역할·그룹·동의 관리 API")
class OperationAccountController(
    private val useCase: OperationAccountService,
    private val currentAccounts: CurrentAccountService,
    private val webAuthentication: WebAuthenticationService,
) {
    @Operation(summary = "계정에 부여된 역할 조회", description = "계정에 부여된 역할 조회 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @GetMapping("/roles")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun managedRoles() = useCase.managedRoles().map { it.toResponse() }

    @Operation(summary = "부여 가능한 역할 목록 조회", description = "부여 가능한 역할 목록 조회 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @GetMapping("/{id}/roles")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun roles(@Parameter(description = "리소스 공개 식별자(UUID)") @PathVariable id: UUID) = useCase.roles(id).map { it.toResponse() }

    @Operation(summary = "계정 역할 부여", description = "계정 역할 부여 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @PutMapping("/{id}/roles/{roleCode}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun grantRole(@Parameter(description = "리소스 공개 식별자(UUID)") @PathVariable id: UUID, @Parameter(description = "계정 역할 코드") @PathVariable roleCode: String) =
        useCase.grantRole(id, roleCode, currentAccounts.activeAccountPublicId()).map { it.toResponse() }

    @Operation(summary = "계정 역할 회수", description = "계정 역할 회수 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @DeleteMapping("/{id}/roles/{roleCode}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun revokeRole(@Parameter(description = "리소스 공개 식별자(UUID)") @PathVariable id: UUID, @Parameter(description = "계정 역할 코드") @PathVariable roleCode: String) =
        useCase.revokeRole(id, roleCode).map { it.toResponse() }

    @Operation(summary = "주문 생성", description = "주문 생성 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @PostMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateOperationAccountRequest) = useCase.create(
        NewAccount(
            request.name.trim(),
            request.phone.trim(),
            PhoneNumberHelper.normalizeMobilePhoneNumber(request.phone),
            request.email.clean(),
            request.businessProfile?.toData(),
        ),
    ).toResponse()

    @Operation(summary = "운영 계정 목록 조회", description = "운영 계정 목록 조회 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @GetMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun list(
        @Parameter(description = "조회할 상태 코드") @RequestParam(required = false) status: String?,
        @Parameter(description = "조회할 페이지 번호(0부터 시작)") @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "페이지당 조회할 항목 수") @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ) =
        useCase.list(status, page, size).map { it.toResponse() }

    @Operation(summary = "운영 계정 상세 조회", description = "운영 계정 상세 조회 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @GetMapping("/{id}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun detail(@Parameter(description = "리소스 공개 식별자(UUID)") @PathVariable id: UUID) = useCase.detail(id).toResponse()

    @Operation(summary = "계정 상태 변경", description = "계정 상태 변경 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @PatchMapping("/{id}/status")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun status(
        @Parameter(description = "리소스 공개 식별자(UUID)") @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateAccountStatusRequest,
    ) = useCase.status(id, request.status).toResponse()

    @Operation(summary = "사용자 프로필수정", description = "운영자 계정·상태·역할·그룹·동의 관리 API. /{id}/business-profile 경로에서 사용자 프로필수정를 수행")
    @PutMapping("/{id}/business-profile")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun profile(
        @Parameter(description = "리소스 공개 식별자(UUID)") @PathVariable id: UUID,
        @Valid @RequestBody request: BusinessProfileRequest,
    ) = useCase.profile(id, request.toData()).toResponse()

    @Operation(summary = "구매자 그룹수정", description = "운영자 계정·상태·역할·그룹·동의 관리 API. /{id}/buyer-group 경로에서 구매자 그룹수정를 수행")
    @PutMapping("/{id}/buyer-group")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun assignBuyerGroup(@Parameter(description = "리소스 공개 식별자(UUID)") @PathVariable id: UUID, @Valid @RequestBody request: AssignBuyerGroupRequest) =
        useCase.assignBuyerGroup(id, request.buyerGroupId).toResponse()

    @Operation(summary = "계정 약관 동의 등록", description = "계정 약관 동의 등록 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @PostMapping("/{id}/consents")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun consent(
        @Parameter(description = "리소스 공개 식별자(UUID)") @PathVariable id: UUID,
        @Valid @RequestBody request: CreateConsentRequest,
    ) = useCase.consent(
        id,
        ConsentCommand(
            request.consentType,
            request.documentVersion,
            request.consentMethod,
            request.evidenceReference.clean(),
            currentAccounts.activeAccountPublicId(),
        ),
    ).toResponse()

    @Operation(summary = "계정 승인", description = "계정 승인 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @PostMapping("/{id}/approve")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun approve(
        @Parameter(description = "리소스 공개 식별자(UUID)") @PathVariable id: UUID,
    ) = useCase.approve(id).toResponse()

    @Operation(summary = "관리자 TOTP 초기화", description = "MFA 인증된 운영자가 대상 관리자 Authenticator 등록을 초기화하고 기존 token session을 폐기")
    @PostMapping("/{id}/totp/reset")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun resetAdminTotp(@Parameter(description = "리소스 공개 식별자(UUID)") @PathVariable id: UUID) = webAuthentication.resetTotp(id)

    private fun BusinessProfileRequest.toData() = BusinessProfileData(
        businessName.trim(),
        businessRegistrationNumber.clean(),
        representativeName.clean(),
        businessPhone.clean(),
        postalCode.clean(),
        address1.clean(),
        address2.clean(),
    )
    private fun BusinessProfileData.toResponse() = OperationBusinessProfileResponse(
        businessName,
        businessRegistrationNumber,
        representativeName,
        businessPhone,
        postalCode,
        address1,
        address2,
        status,
    )
    private fun ManagedRole.toResponse() = OperationRoleResponse(code, name)
    private fun AccountData.toResponse() = OperationAccountResponse(
        id,
        name,
        phone,
        email,
        status,
        tokenVersion,
        profile?.toResponse(),
        consents.map {
            OperationConsentResponse(it.consentType, it.documentVersion, it.consentMethod, it.evidenceReference, it.processedBy, it.consentedAt)
        },
        buyerGroupId,
    )
    private fun String?.clean() = this?.trim()?.ifBlank { null }
}
