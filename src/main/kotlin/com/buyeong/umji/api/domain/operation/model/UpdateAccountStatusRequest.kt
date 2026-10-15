package com.buyeong.umji.api.domain.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@Schema(description = "UpdateAccountStatusRequest API 데이터 모델")
data class UpdateAccountStatusRequest(
    @field:NotBlank @field:Pattern(regexp = "PENDING_CONSENT|PENDING_PROFILE|PENDING_REVIEW|ACTIVE|SUSPENDED|WITHDRAWN")
    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true)
    val status: String,
)
