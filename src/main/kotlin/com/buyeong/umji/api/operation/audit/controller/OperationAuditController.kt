package com.buyeong.umji.api.operation.audit.controller

import com.buyeong.umji.api.operation.audit.model.OperationAuditEntry
import com.buyeong.umji.api.operation.audit.model.OperationAuditQuery
import com.buyeong.umji.api.operation.audit.service.OperationAuditService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.Instant
import java.util.UUID
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/operation/audit-logs")
@Validated
@Tag(name = "운영 감사 로그", description = "운영자 변경 이력 검색 API")
class OperationAuditController(private val audit: OperationAuditService) {
    @Operation(summary = "운영 감사 로그 검색", description = "운영 감사 로그 검색 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @GetMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_AUDIT_READ')")
    fun search(
        @Parameter(description = "조회할 운영자 공개 식별자(UUID)") @RequestParam(required = false) actorId: UUID?,
        @Parameter(description = "조회할 리소스 유형 코드") @RequestParam(required = false) resourceType: String?,
        @Parameter(description = "검색 시작 시각(ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @Parameter(description = "검색 종료 시각(ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) until: Instant?,
        @Parameter(description = "조회할 페이지 번호(0부터 시작)") @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "페이지당 조회할 항목 수") @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ) = audit.search(OperationAuditQuery(actorId, resourceType, from, until, page, size)).let { result ->
        OperationAuditPageResponse(result.items.map { it.toResponse() }, result.page, result.size, result.totalElements, result.totalPages)
    }

    private fun OperationAuditEntry.toResponse() = OperationAuditEntryResponse(
        id,
        actorId,
        action,
        resourceType,
        resourceId,
        requestTraceId,
        occurredAt,
    )
}

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

@Schema(description = "운영 감사 로그 검색 결과와 페이지 정보")
data class OperationAuditPageResponse(
    @field:Schema(description = "현재 페이지의 감사 이력 목록", example = "[]", type = "array", required = true)
    val items: List<OperationAuditEntryResponse>,
    @field:Schema(description = "페이지 번호(0부터 시작)", example = "0", format = "int32", type = "integer", required = true)
    val page: Int,
    @field:Schema(description = "페이지당 항목 수", example = "20", format = "int32", type = "integer", required = true)
    val size: Int,
    @field:Schema(description = "전체 검색 결과 수", example = "1", format = "int64", type = "integer", required = true)
    val totalElements: Long,
    @field:Schema(description = "전체 페이지 수", example = "1", format = "int32", type = "integer", required = true)
    val totalPages: Int,
)
