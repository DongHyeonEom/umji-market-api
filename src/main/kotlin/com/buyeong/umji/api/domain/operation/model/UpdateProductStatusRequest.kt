package com.buyeong.umji.api.domain.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@Schema(description = "UpdateProductStatusRequest API 데이터 모델")
data class UpdateProductStatusRequest(
    @field:NotBlank @field:Pattern(regexp = "DISPLAYED|HIDDEN")
    @field:Schema(description = "화면 노출 상태 코드", example = "예시 값", type = "string", required = true)
    val displayStatus: String,

    @field:NotBlank @field:Pattern(regexp = "ON_SALE|STOPPED")
    @field:Schema(description = "판매 상태 코드", example = "예시 값", type = "string", required = true)
    val salesStatus: String,
)
