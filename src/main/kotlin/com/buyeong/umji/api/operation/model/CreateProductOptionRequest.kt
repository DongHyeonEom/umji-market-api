package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

@Schema(description = "CreateProductOptionRequest API 데이터 모델")
data class CreateProductOptionRequest(
    @field:NotBlank @field:Size(max = 100,)
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,

    @field:Min(
        0,
    ) @field:Schema(description = "화면 표시 순서", example = "1", format = "int32", type = "integer", required = false, implementation = Int::class)
    val displayOrder: Int = 0,

    @field:NotEmpty @field:ArraySchema(
        schema = Schema(implementation = CreateProductOptionValueRequest::class),
    ) @field:Schema(description = "상품 옵션 값 목록", example = "[]", type = "array", required = true)
    val values: List<@Valid CreateProductOptionValueRequest>,
)