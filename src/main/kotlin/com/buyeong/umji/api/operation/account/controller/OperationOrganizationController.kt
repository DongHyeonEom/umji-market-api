package com.buyeong.umji.api.operation.account.controller

import com.buyeong.umji.api.account.model.OrganizationTaxInvoiceProfileRequest
import com.buyeong.umji.api.account.model.SetOrganizationRepresentativeRequest
import com.buyeong.umji.api.account.model.toResponse as taxInvoiceProfileResponse
import com.buyeong.umji.api.account.service.OrganizationTaxInvoiceProfileService
import com.buyeong.umji.api.operation.account.service.OperationAccountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/operation/organizations")
@Tag(name = "운영 Organization", description = "Organization 대표자와 구매 사업자 정보 운영 API")
class OperationOrganizationController(
    private val accounts: OperationAccountService,
    private val taxInvoiceProfiles: OrganizationTaxInvoiceProfileService,
) {
    @Operation(summary = "Organization수정", description = "Organization 대표자 지정 등 그룹 운영 API. /{organizationId}/representative 경로에서 Organization수정를 수행")
    @PutMapping("/{organizationId}/representative")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun setRepresentative(
        @Parameter(description = "검색 조건: organizationId") @PathVariable organizationId: UUID,
        @Valid @RequestBody request: SetOrganizationRepresentativeRequest,
    ) = accounts.setOrganizationRepresentative(organizationId, request.accountId)

    @Operation(summary = "그룹 세금계산서 정보 조회", description = "Organization의 공급받는자 세금계산서 정보를 조회")
    @GetMapping("/{organizationId}/tax-invoice-profile")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun taxInvoiceProfile(@Parameter(description = "Organization 공개 식별자(UUID)") @PathVariable organizationId: UUID) =
        taxInvoiceProfiles.forGroup(organizationId).taxInvoiceProfileResponse()

    @Operation(summary = "그룹 세금계산서 정보 수정", description = "Organization의 공급받는자 세금계산서 정보를 수정")
    @PutMapping("/{organizationId}/tax-invoice-profile")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun updateTaxInvoiceProfile(
        @Parameter(description = "Organization 공개 식별자(UUID)") @PathVariable organizationId: UUID,
        @Valid @RequestBody request: OrganizationTaxInvoiceProfileRequest,
    ) = taxInvoiceProfiles.updateForGroup(organizationId, request.toCommand()).taxInvoiceProfileResponse()
}
