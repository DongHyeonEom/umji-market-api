package com.buyeong.umji.api.operation.account.adapter.`in`.web

import com.buyeong.umji.api.account.model.SetBuyerGroupRepresentativeRequest
import com.buyeong.umji.api.account.model.BuyerGroupTaxInvoiceProfileRequest
import com.buyeong.umji.api.account.model.toResponse as taxInvoiceProfileResponse
import com.buyeong.umji.api.account.application.BuyerGroupTaxInvoiceProfileService
import com.buyeong.umji.api.operation.account.application.OperationAccountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/operation/buyer-groups")
@Tag(name = "운영 구매자 그룹", description = "구매자 그룹 대표자 지정 등 그룹 운영 API")
class OperationBuyerGroupController(
    private val accounts: OperationAccountService,
    private val taxInvoiceProfiles: BuyerGroupTaxInvoiceProfileService,
) {
    @Operation(summary = "구매자 그룹수정", description = "구매자 그룹 대표자 지정 등 그룹 운영 API. /{groupId}/representative 경로에서 구매자 그룹수정를 수행")
    @PutMapping("/{groupId}/representative")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun setRepresentative(
        @Parameter(description = "검색 조건: groupId") @PathVariable groupId: UUID,
        @Valid @RequestBody request: SetBuyerGroupRepresentativeRequest,
    ) = accounts.setBuyerGroupRepresentative(groupId, request.accountId)

    @Operation(summary = "그룹 세금계산서 정보 조회", description = "구매자 그룹의 공급받는자 세금계산서 정보를 조회")
    @GetMapping("/{groupId}/tax-invoice-profile")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun taxInvoiceProfile(@Parameter(description = "구매자 그룹 공개 식별자(UUID)") @PathVariable groupId: UUID) =
        taxInvoiceProfiles.forGroup(groupId).taxInvoiceProfileResponse()

    @Operation(summary = "그룹 세금계산서 정보 수정", description = "구매자 그룹의 공급받는자 세금계산서 정보를 수정")
    @PutMapping("/{groupId}/tax-invoice-profile")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_ACCOUNT_MANAGE')")
    fun updateTaxInvoiceProfile(
        @Parameter(description = "구매자 그룹 공개 식별자(UUID)") @PathVariable groupId: UUID,
        @Valid @RequestBody request: BuyerGroupTaxInvoiceProfileRequest,
    ) = taxInvoiceProfiles.updateForGroup(groupId, request.toCommand()).taxInvoiceProfileResponse()
}
