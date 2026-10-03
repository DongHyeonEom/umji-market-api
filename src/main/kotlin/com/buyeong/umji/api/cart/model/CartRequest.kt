package com.buyeong.umji.api.cart.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "AddCartItemRequest API 데이터 모델")
data class AddCartItemRequest(
    @field:NotNull @field:Schema(
        description = "상품 옵션(SKU) 공개 식별자(UUID)",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
        required = true,
    ) val skuId: UUID,
    @field:Min(1) @field:Schema(description = "수량", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val quantity: Int,
)

@Schema(description = "UpdateCartItemRequest API 데이터 모델")
data class UpdateCartItemRequest(
    @field:Min(1) @field:Schema(description = "수량", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val quantity: Int,
)