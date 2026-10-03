package com.buyeong.umji.api.payment.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "PaymentStatusRequest API 데이터 모델")
data class PaymentStatusRequest(
    @field:NotBlank
    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true) val status: String,
)