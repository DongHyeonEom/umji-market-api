package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "CreateProductOptionValueRequest API 데이터 모델")
data class CreateProductOptionValueRequest(
    @field:NotBlank @field:Size(max = 100,)
    @field:Schema(description = "값", example = "예시 값", type = "string", required = true)
    val value: String,

    @field:Min(
        0,
    ) @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = false, implementation = Int::class)
    val displayOrder: Int = 0,
)