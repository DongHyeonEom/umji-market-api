package com.buyeong.umji.api.sales.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.exception.InvalidRequestParameterException
import com.buyeong.umji.api.sales.model.SalesCommissionPage
import com.buyeong.umji.api.sales.model.SalesCommissionSettlementResult
import com.buyeong.umji.api.sales.model.SalesCommissionView
import com.buyeong.umji.api.sales.service.SalesCommissionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

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

    private fun SalesCommissionPage.toResponse() = SalesCommissionPageResponse(
        items.map { it.toResponse() },
        page,
        size,
        totalElements,
        totalPages,
    )

    private fun SalesCommissionSettlementResult.toResponse() = SalesCommissionSettlementResponse(month.toString(), payableCount, payableAmount)

    private fun SalesCommissionView.toResponse() = SalesCommissionItemResponse(
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

@Schema(description = "주문 인센티브 페이지와 페이지 정보")
data class SalesCommissionPageResponse(
    @field:Schema(description = "현재 페이지의 주문 인센티브 목록", example = "[]", type = "array", required = true)
    val items: List<SalesCommissionItemResponse>,

    @field:Schema(description = "페이지 번호(0부터 시작)", example = "0", type = "integer", required = true)
    val page: Int,

    @field:Schema(description = "페이지당 항목 수", example = "20", type = "integer", required = true)
    val size: Int,

    @field:Schema(description = "전체 인센티브 건수", example = "1", type = "integer", required = true)
    val totalElements: Long,

    @field:Schema(description = "전체 페이지 수", example = "1", type = "integer", required = true)
    val totalPages: Int,
)

@Schema(description = "주문 시점의 영업 담당·요율·인센티브 snapshot과 정산 상태")
data class SalesCommissionItemResponse(
    @field:Schema(description = "인센티브 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", type = "string", format = "uuid", required = true)
    val id: UUID,

    @field:Schema(description = "대상 주문 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000002", type = "string", format = "uuid", required = true)
    val orderId: UUID,

    @field:Schema(description = "담당 영업 계정 공개 식별자(UUID). 담당자 없으면 null", example = "00000000-0000-0000-0000-000000000003", type = "string", format = "uuid", nullable = true, required = false)
    val salesAccountId: UUID?,

    @field:Schema(description = "주문 시점 인센티브율(basis points). 미설정이면 null", example = "30", type = "integer", nullable = true, required = false)
    val rateBps: Int?,

    @field:Schema(description = "세금·배송비 제외 상품 순판매 기준액(KRW)", example = "100000", type = "integer", format = "int64", required = true)
    val basisAmount: Long,

    @field:Schema(description = "정수 원화 HALF_UP 계산 인센티브(KRW)", example = "300", type = "integer", format = "int64", required = true)
    val commissionAmount: Long,

    @field:Schema(description = "정산 상태", example = "WAITING", type = "string", required = true)
    val status: String,

    @field:Schema(description = "월말 정산 대상 월의 첫 날짜", example = "2026-09-01", type = "string", format = "date", nullable = true, required = false)
    val settlementMonth: LocalDate?,

    @field:Schema(description = "배송완료와 전액 입금 중 늦은 자격 충족 시각", example = "2026-09-30T10:00:00Z", type = "string", format = "date-time", nullable = true, required = false)
    val qualifiedAt: Instant?,

    @field:Schema(description = "실제 지급 완료 시각", example = "2026-10-01T03:00:00Z", type = "string", format = "date-time", nullable = true, required = false)
    val paidAt: Instant?,

    @field:Schema(description = "인센티브 snapshot 생성 시각", example = "2026-09-01T03:00:00Z", type = "string", format = "date-time", required = true)
    val createdAt: Instant,
)

@Schema(description = "월말 인센티브 정산 처리 결과")
data class SalesCommissionSettlementResponse(
    @field:Schema(description = "정산 대상 월", example = "2026-09", type = "string", required = true)
    val month: String,

    @field:Schema(description = "이번 실행에서 PAYABLE로 전환한 건수", example = "12", type = "integer", required = true)
    val payableCount: Int,

    @field:Schema(description = "이번 실행에서 PAYABLE로 전환한 인센티브 합계(KRW)", example = "36000", type = "integer", format = "int64", required = true)
    val payableAmount: Long,
)
