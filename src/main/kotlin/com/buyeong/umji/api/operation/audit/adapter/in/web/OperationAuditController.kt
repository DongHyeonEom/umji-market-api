package com.buyeong.umji.api.operation.audit.adapter.`in`.web

import com.buyeong.umji.api.operation.audit.application.model.OperationAuditEntry
import com.buyeong.umji.api.operation.audit.application.model.OperationAuditQuery
import com.buyeong.umji.api.operation.audit.application.port.`in`.OperationAuditUseCase
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
class OperationAuditController(private val audit: OperationAuditUseCase) {
    @GetMapping
    @PreAuthorize("@operationAuthorization.hasPermission(authentication, 'ADMIN_AUDIT_READ')")
    fun search(
        @RequestParam(required = false) actorId: UUID?,
        @RequestParam(required = false) resourceType: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) until: Instant?,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
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

data class OperationAuditEntryResponse(
    val id: Long,
    val actorId: UUID?,
    val action: String,
    val resourceType: String,
    val resourceId: UUID?,
    val requestTraceId: String?,
    val occurredAt: Instant,
)

data class OperationAuditPageResponse(
    val items: List<OperationAuditEntryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)