package com.buyeong.umji.api.domain.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "Organization 구매 정책 변경 요청")
data class UpdateOrganizationCommercePolicyRequest(
    @field:NotBlank
    @field:Pattern(regexp = "WHOLESALE_BUSINESS|GENERAL_BUSINESS|INDIVIDUAL")
    @field:Schema(description = "구매 유형", example = "WHOLESALE_BUSINESS", required = true)
    val buyerType: String,

    @field:Schema(description = "전액 입금 전 배송 준비 제한 여부", example = "true", required = true)
    val paymentBeforeShipmentRequired: Boolean,

    @field:NotBlank @field:Size(max = 500)
    @field:Schema(description = "정책 변경 사유", example = "거래 조건 변경", required = true)
    val reason: String,
)
