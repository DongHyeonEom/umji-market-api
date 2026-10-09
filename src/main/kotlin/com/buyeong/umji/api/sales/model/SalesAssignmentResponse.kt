package com.buyeong.umji.api.sales.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

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