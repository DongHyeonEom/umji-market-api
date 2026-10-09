package com.buyeong.umji.api.domain.operation.audit.dto

import java.time.Instant
import java.util.UUID

data class OperationAuditEntryDto(
    val id: Long,
    val actorId: UUID?,
    val action: String,
    val resourceType: String,
    val resourceId: UUID?,
    val requestTraceId: String?,
    val occurredAt: Instant,
)