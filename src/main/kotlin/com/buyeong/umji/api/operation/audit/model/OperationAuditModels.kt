package com.buyeong.umji.api.operation.audit.model

import java.time.Instant
import java.util.UUID

data class OperationAuditEvent(
    val actorId: UUID?,
    val action: String,
    val resourceType: String,
    val resourceId: UUID?,
    val requestTraceId: String?,
    val occurredAt: Instant,
)

data class OperationAuditEntry(
    val id: Long,
    val actorId: UUID?,
    val action: String,
    val resourceType: String,
    val resourceId: UUID?,
    val requestTraceId: String?,
    val occurredAt: Instant,
)

data class OperationAuditQuery(
    val actorId: UUID?,
    val resourceType: String?,
    val from: Instant?,
    val until: Instant?,
    val page: Int,
    val size: Int,
)

data class OperationAuditPage(
    val items: List<OperationAuditEntry>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)