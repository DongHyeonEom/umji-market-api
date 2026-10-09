package com.buyeong.umji.api.operation.audit.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "운영자가 수행한 변경 이력 항목")
data class OperationAuditEntryResponse(
    @field:Schema(description = "감사 로그 일련 번호", example = "1", format = "int64", type = "integer", required = true)
    val id: Long,

    @field:Schema(description = "작업을 수행한 운영자 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", type = "string", required = false)
    val actorId: UUID?,

    @field:Schema(description = "수행한 작업 코드", example = "ACCOUNT_STATUS_CHANGED", type = "string", required = true)
    val action: String,

    @field:Schema(description = "변경 대상 리소스 유형", example = "ACCOUNT", type = "string", required = true)
    val resourceType: String,

    @field:Schema(description = "변경 대상 리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", type = "string", required = false)
    val resourceId: UUID?,

    @field:Schema(description = "요청 추적 식별자", example = "trace-id", type = "string", required = false)
    val requestTraceId: String?,

    @field:Schema(description = "작업 발생 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true)
    val occurredAt: Instant,
)