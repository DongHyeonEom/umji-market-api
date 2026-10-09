package com.buyeong.umji.api.cart.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "구매자 그룹의 장바구니 정보")
data class CartResponse(
    @field:ArraySchema(
        schema = Schema(implementation = CartItemResponse::class),
    ) @field:Schema(description = "현재 페이지의 항목 목록", example = "[]", type = "array", required = true)
    val items: List<CartItemResponse>,
)