package com.buyeong.umji.api.domain.payment.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "PaymentStatusRequest API 데이터 모델")
data class PaymentStatusRequest(
    @field:NotBlank @field:Pattern(regexp = "WAITING_FOR_DEPOSIT|PARTIAL_PAYMENT_REVIEW_REQUIRED|PAYMENT_ISSUE_REVIEW_REQUIRED|PAYMENT_CONFIRMED|REFUND_PENDING|REFUNDED")
    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true)
    val status: String,

    @field:NotBlank
    @field:Size(max = 500,)
    val reason: String,
)
