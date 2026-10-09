package com.buyeong.umji.api.domain.operation.audit.dto

import java.time.Instant
import java.util.UUID

data class OperationAuditQueryDto(
    val actorId: UUID?,
    val resourceType: String?,
    val from: Instant?,
    val until: Instant?,
    val page: Int,
    val size: Int,
)