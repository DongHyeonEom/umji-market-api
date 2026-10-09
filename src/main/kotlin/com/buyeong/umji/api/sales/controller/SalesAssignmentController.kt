package com.buyeong.umji.api.sales.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.sales.dto.SalesAssignmentCommandDto
import com.buyeong.umji.api.sales.dto.SalesAssignmentViewDto
import com.buyeong.umji.api.sales.model.SalesAssignmentRequest
import com.buyeong.umji.api.sales.model.SalesAssignmentResponse
import com.buyeong.umji.api.sales.service.SalesAssignmentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/operation/organizations/{organizationId}/sales-assignment")
@Tag(name = "운영 영업 담당 배정", description = "구매 Organization의 영업 담당자 및 선택 인센티브율 이력 관리 API")
class SalesAssignmentController(
    private val assignments: SalesAssignmentService,
    private val currentAccounts: CurrentAccountService,
) {
    @Operation(summary = "영업 담당 배정 이력 조회", description = "Organization의 담당 영업자와 적용 요율의 전체 유효기간 이력 조회")
    @GetMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SALES_GROUP_READ')")
    fun history(
        @Parameter(description = "담당 배정 이력을 조회할 구매 Organization 공개 식별자(UUID)")
        @PathVariable organizationId: UUID,
    ) = assignments.history(organizationId).map { it.toResponse() }

    @Operation(summary = "영업 담당자 및 인센티브율 배정", description = "현재 배정을 종료하고 새 담당자·선택 요율의 이력을 추가")
    @PutMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SALES_GROUP_ASSIGN')")
    fun assign(
        @Parameter(description = "담당 배정을 변경할 구매 Organization 공개 식별자(UUID)")
        @PathVariable organizationId: UUID,
        @Valid @RequestBody request: SalesAssignmentRequest,
    ) = assignments.assign(
        organizationId,
        SalesAssignmentCommandDto(
            request.salesAccountId,
            request.commissionRateBps,
            request.assignmentReason.trim(),
            currentAccounts.activeAccountPublicId(),
        ),
    ).map { it.toResponse() }

    private fun SalesAssignmentViewDto.toResponse() = SalesAssignmentResponse(
        id,
        salesAccountId,
        salesAccountName,
        commissionRateBps,
        assignmentReason,
        validFrom,
        validUntil,
        assignedByAccountId,
    )
}