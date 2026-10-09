package com.buyeong.umji.api.sales.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Schema(description = "주문 시점의 영업 담당·요율·인센티브 snapshot과 정산 상태")
data class SalesCommissionItemResponse(
    @field:Schema(description = "인센티브 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", type = "string", format = "uuid", required = true)
    val id: UUID,

    @field:Schema(description = "대상 주문 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000002", type = "string", format = "uuid", required = true)
    val orderId: UUID,

    @field:Schema(
        description = "담당 영업 계정 공개 식별자(UUID). 담당자 없으면 null",
        example = "00000000-0000-0000-0000-000000000003",
        type = "string",
        format = "uuid",
        nullable = true,
        required = false
    )
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