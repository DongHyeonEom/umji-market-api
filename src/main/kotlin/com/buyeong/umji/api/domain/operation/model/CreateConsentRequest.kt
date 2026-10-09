package com.buyeong.umji.api.domain.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "CreateConsentRequest API 데이터 모델")
data class CreateConsentRequest(
    @field:NotBlank @field:Size(max = 100,)
    @field:Schema(description = "Consent Type 정보", example = "예시 값", type = "string", required = true)
    val consentType: String,

    @field:NotBlank @field:Size(max = 100,)
    @field:Schema(description = "Document Version 정보", example = "예시 값", type = "string", required = true)
    val documentVersion: String,

    @field:NotBlank
    @field:Schema(description = "Consent Method 정보", example = "예시 값", type = "string", required = true)
    val consentMethod: String,

    @field:Size(max = 500,)
    @field:Schema(description = "Evidence Reference 정보", example = "예시 값", type = "string", required = false)
    val evidenceReference: String? = null,
)