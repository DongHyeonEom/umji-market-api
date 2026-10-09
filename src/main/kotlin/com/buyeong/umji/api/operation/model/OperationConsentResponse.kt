package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "OperationConsentResponse API 데이터 모델")
data class OperationConsentResponse(
    @field:Schema(description = "Consent Type 정보", example = "예시 값", type = "string", required = true)
    val consentType: String,

    @field:Schema(description = "Document Version 정보", example = "예시 값", type = "string", required = true)
    val documentVersion: String,

    @field:Schema(description = "Consent Method 정보", example = "예시 값", type = "string", required = true)
    val consentMethod: String,

    @field:Schema(description = "Evidence Reference 정보", example = "예시 값", type = "string", required = true)
    val evidenceReference: String?,

    @field:Schema(description = "Processed By 정보", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val processedBy: UUID?,

    @field:Schema(description = "Consented At 정보", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true)
    val consentedAt: Instant,
)