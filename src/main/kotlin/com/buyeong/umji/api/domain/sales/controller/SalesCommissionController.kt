package com.buyeong.umji.api.domain.sales.controller

import com.buyeong.umji.api.domain.auth.service.CurrentAccountService
import com.buyeong.umji.api.domain.sales.dto.SalesCommissionPageDto
import com.buyeong.umji.api.domain.sales.dto.SalesCommissionSettlementResultDto
import com.buyeong.umji.api.domain.sales.dto.SalesCommissionViewDto
import com.buyeong.umji.api.domain.sales.model.SalesCommissionItemResponse
import com.buyeong.umji.api.domain.sales.model.SalesCommissionPageResponse
import com.buyeong.umji.api.domain.sales.model.SalesCommissionSettlementResponse
import com.buyeong.umji.api.domain.sales.service.SalesCommissionService
import com.buyeong.umji.api.exception.InvalidRequestParameterException
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth
import java.util.UUID

@RestController
@RequestMapping("/api/operation/sales-commissions")
@Validated
@Tag(name = "운영 영업 인센티브", description = "본인·전체 인센티브 조회, 월말 정산 및 지급 처리 API")
class SalesCommissionController(
    private val commissions: SalesCommissionService,
    private val currentAccounts: CurrentAccountService,
) {
    @Operation(summary = "본인 인센티브 조회", description = "인증된 영업 계정에 귀속된 주문 snapshot과 정산 상태를 페이지 조회")
    @GetMapping("/me")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SALES_COMMISSION_READ')")
    fun mine(
        @Parameter(description = "조회할 페이지 번호(0부터 시작)") @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "페이지당 항목 수(1~100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ) = commissions.mine(currentAccounts.activeAccountPublicId(), page, size).toResponse()

    @Operation(summary = "전체 인센티브 조회", description = "전체 주문 인센티브 snapshot과 정산 상태를 페이지 조회")
    @GetMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SALES_COMMISSION_READ_ALL')")
    fun all(
        @Parameter(description = "조회할 페이지 번호(0부터 시작)") @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "페이지당 항목 수(1~100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ) = commissions.all(page, size).toResponse()

    @Operation(summary = "월말 인센티브 정산", description = "배송완료·전액 입금 주문을 지정 월의 PAYABLE 상태로 멱등 전환")
    @PostMapping("/settlements/{month}")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SALES_COMMISSION_SETTLE')")
    fun settle(
        @Parameter(description = "정산 대상 월(YYYY-MM). 마감된 과거 월만 허용", example = "2026-09") @PathVariable month: String,
    ) = commissions.settle(parseMonth(month), currentAccounts.activeAccountPublicId()).toResponse()

    @Operation(summary = "인센티브 지급 완료 처리", description = "PAYABLE 인센티브의 실제 지급을 기록하고 PAID로 변경")
    @PutMapping("/{commissionId}/paid")
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'SALES_COMMISSION_SETTLE')")
    fun markPaid(
        @Parameter(description = "인센티브 공개 식별자(UUID)") @PathVariable commissionId: UUID,
    ) = commissions.markPaid(commissionId, currentAccounts.activeAccountPublicId()).toResponse()

    private fun parseMonth(value: String): YearMonth = try {
        YearMonth.parse(value)
    } catch (_: RuntimeException) {
        throw InvalidRequestParameterException("정산 월은 YYYY-MM 형식이어야 합니다.")
    }

    private fun SalesCommissionPageDto.toResponse() = SalesCommissionPageResponse(
        items.map { it.toResponse() },
        page,
        size,
        totalElements,
        totalPages,
    )

    private fun SalesCommissionSettlementResultDto.toResponse() = SalesCommissionSettlementResponse(month.toString(), payableCount, payableAmount)

    private fun SalesCommissionViewDto.toResponse() = SalesCommissionItemResponse(
        id,
        orderId,
        salesAccountId,
        rateBps,
        basisAmount,
        commissionAmount,
        status,
        settlementMonth,
        qualifiedAt,
        paidAt,
        createdAt,
    )
}