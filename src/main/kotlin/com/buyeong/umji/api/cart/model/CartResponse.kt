package com.buyeong.umji.api.cart.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "구매자 그룹의 장바구니 정보")
data class CartResponse(
    @field:ArraySchema(
        schema = Schema(implementation = CartItemResponse::class),
    ) @field:Schema(description = "현재 페이지의 항목 목록", example = "[]", type = "array", required = true) val items: List<CartItemResponse>,
)

@Schema(description = "장바구니에 담긴 상품 옵션과 수량 정보")
data class CartItemResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val id: UUID,
    @field:Schema(description = "상품 옵션(SKU) 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val skuId: UUID,
    @field:Schema(description = "상품 옵션 코드", example = "예시 값", type = "string", required = true) val skuCode: String,
    @field:Schema(description = "상품명", example = "예시 값", type = "string", required = true) val productName: String,
    @field:Schema(description = "상품 옵션명", example = "예시 값", type = "string", required = true) val skuName: String,
    @field:Schema(description = "수량", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val quantity: Int,
    @field:Schema(description = "Current Sale Price 정보", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val currentSalePrice:
    Long,
    @field:Schema(description = "판매 상태 코드", example = "예시 값", type = "string", required = true) val salesStatus: String,
)