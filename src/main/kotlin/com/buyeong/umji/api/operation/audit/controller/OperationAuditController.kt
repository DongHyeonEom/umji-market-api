package com.buyeong.umji.api.operation.audit.controller

import com.buyeong.umji.api.operation.audit.dto.OperationAuditEntryDto
import com.buyeong.umji.api.operation.audit.dto.OperationAuditQueryDto
import com.buyeong.umji.api.operation.audit.model.OperationAuditEntryResponse
import com.buyeong.umji.api.operation.audit.model.OperationAuditPageResponse
import com.buyeong.umji.api.operation.audit.service.OperationAuditService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

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
    ) = audit.search(OperationAuditQueryDto(actorId, resourceType, from, until, page, size)).let { result ->
        OperationAuditPageResponse(result.items.map { it.toResponse() }, result.page, result.size, result.totalElements, result.totalPages)
    }

    private fun OperationAuditEntryDto.toResponse() = OperationAuditEntryResponse(
        id,
        actorId,
        action,
        resourceType,
        resourceId,
        requestTraceId,
        occurredAt,
    )
}