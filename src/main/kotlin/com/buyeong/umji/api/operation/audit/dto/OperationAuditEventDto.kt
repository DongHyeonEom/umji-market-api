package com.buyeong.umji.api.operation.audit.dto

import java.time.Instant
import java.util.UUID

data class OperationAuditEventDto(
    val actorId: UUID?,
    val action: String,
    val resourceType: String,
    val resourceId: UUID?,
    val requestTraceId: String?,
    val occurredAt: Instant,
)