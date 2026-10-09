package com.buyeong.umji.api.sales.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.sales.model.SalesAssignmentCommand
import com.buyeong.umji.api.sales.model.SalesAssignmentView
import com.buyeong.umji.api.sales.service.SalesAssignmentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
        SalesAssignmentCommand(
            request.salesAccountId,
            request.commissionRateBps,
            request.assignmentReason.trim(),
            currentAccounts.activeAccountPublicId(),
        ),
    ).map { it.toResponse() }

    private fun SalesAssignmentView.toResponse() = SalesAssignmentResponse(
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

@Schema(description = "구매 Organization 영업 담당자 배정 요청")
data class SalesAssignmentRequest(
    @field:Schema(description = "담당 영업 계정 공개 식별자(UUID). 활성 SALES_MANAGER 계정만 배정 가능", example = "00000000-0000-0000-0000-000000000001", type = "string", required = true)
    val salesAccountId: UUID,
    @field:Schema(description = "선택 인센티브율(basis points). null은 인센티브 미설정", example = "30", type = "integer", required = false, nullable = true)
    @field:Min(1) @field:Max(10_000)
    val commissionRateBps: Int?,
    @field:Schema(description = "배정 또는 변경 사유 코드", example = "INITIAL_ASSIGNMENT", type = "string", required = true)
    @field:NotBlank @field:Size(max = 30)
    val assignmentReason: String,
)

@Schema(description = "영업 담당 배정 이력")
data class SalesAssignmentResponse(
    @field:Schema(description = "배정 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000002", type = "string", required = true)
    val id: UUID,
    @field:Schema(description = "담당 영업 계정 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", type = "string", required = true)
    val salesAccountId: UUID,
    @field:Schema(description = "담당 영업자 표시명", example = "홍길동", type = "string", required = true)
    val salesAccountName: String,
    @field:Schema(description = "적용 인센티브율(basis points). null은 미설정", example = "30", type = "integer", required = false, nullable = true)
    val commissionRateBps: Int?,
    @field:Schema(description = "배정 변경 사유 코드", example = "INITIAL_ASSIGNMENT", type = "string", required = true)
    val assignmentReason: String,
    @field:Schema(description = "적용 시작 시각(ISO-8601)", example = "2026-10-09T00:00:00Z", type = "string", format = "date-time", required = true)
    val validFrom: Instant,
    @field:Schema(description = "적용 종료 시각(ISO-8601). null은 현재 유효", example = "2026-12-31T15:00:00Z", type = "string", format = "date-time", required = false, nullable = true)
    val validUntil: Instant?,
    @field:Schema(description = "배정을 수행한 운영자 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000003", type = "string", required = true)
    val assignedByAccountId: UUID,
)
