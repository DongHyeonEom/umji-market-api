package com.buyeong.umji.api.domain.operation.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "CreateProductSkuRequest API 데이터 모델")
data class CreateProductSkuRequest(
    @field:NotBlank @field:Size(max = 64,)
    @field:Schema(description = "상품 옵션 코드", example = "예시 값", type = "string", required = true)
    val skuCode: String,

    @field:NotBlank @field:Size(max = 200,)
    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,

    @field:Min(0)
    @field:Schema(description = "판매 가격(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class)
    val salePrice: Long,

    @field:Min(
        0,
    ) @field:Schema(description = "정가(원)", example = "1", format = "int64", type = "integer", required = false, implementation = Long::class)
    val listPrice: Long? = null,

    @field:NotBlank
    @field:Schema(description = "판매 상태 코드", example = "예시 값", type = "string", required = false)
    val salesStatus: String = "ON_SALE",

    @field:ArraySchema(
        schema = Schema(implementation = UUID::class),
    ) @field:Schema(description = "SKU에 연결할 상품 옵션 값 공개 식별자(UUID) 목록", example = "[]", type = "array", required = false)
    val optionValueIds: Set<UUID> = emptySet(),
)