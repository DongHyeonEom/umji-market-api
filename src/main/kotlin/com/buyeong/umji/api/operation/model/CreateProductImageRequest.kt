package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "CreateProductImageRequest API 데이터 모델")
data class CreateProductImageRequest(
    @field:NotBlank @field:Size(max = 500,)
    @field:Schema(description = "상품 이미지 저장소 키", example = "예시 값", type = "string", required = true)
    val storageKey: String,

    @field:Size(max = 200,)
    @field:Schema(description = "상품 이미지 대체 텍스트", example = "예시 값", type = "string", required = false)
    val altText: String? = null,

    @field:Min(
        0,
    ) @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = false, implementation = Int::class)
    val displayOrder: Int = 0,
)