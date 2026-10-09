package com.buyeong.umji.api.domain.cart.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

@Schema(description = "UpdateCartItemRequest API 데이터 모델")
data class UpdateCartItemRequest(
    @field:Min(1)
    @field:Schema(description = "판매 단위 수량. WHOLESALE은 박스 수, RETAIL은 낱개 수", example = "2", format = "int32", type = "integer", required = true, implementation = Int::class)
    val quantity: Int,
)