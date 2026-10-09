package com.buyeong.umji.api.cart.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "구매자 그룹의 장바구니 정보")
data class CartResponse(
    @field:ArraySchema(
        schema = Schema(implementation = CartItemResponse::class),
    ) @field:Schema(description = "현재 페이지의 항목 목록", example = "[]", type = "array", required = true)
    val items: List<CartItemResponse>,
)

@Schema(description = "장바구니에 담긴 상품 옵션과 수량 정보")
data class CartItemResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,
    @field:Schema(description = "상품 옵션(SKU) 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val skuId: UUID,
    @field:Schema(description = "상품 옵션 코드", example = "예시 값", type = "string", required = true)
    val skuCode: String,
    @field:Schema(description = "상품명", example = "예시 값", type = "string", required = true)
    val productName: String,
    @field:Schema(description = "상품 옵션명", example = "예시 값", type = "string", required = true)
    val skuName: String,
    @field:Schema(description = "수량", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class)
    val quantity: Int,
    @field:Schema(description = "판매 단위 1개의 현재 가격(원). WHOLESALE은 박스당 가격", example = "12000", format = "int64", type = "integer", required = true, implementation = Long::class)
    val currentSalePrice:
    Long,
    @field:Schema(description = "판매 상태 코드", example = "예시 값", type = "string", required = true)
    val salesStatus: String,
    @field:Schema(description = "판매 오퍼 공개 식별자")
    val salesOfferId: UUID,
    @field:Schema(description = "판매 채널 코드")
    val channelCode: String,
    @field:Schema(
        description = "판매 단위당 기준 SKU 입수 수량. WHOLESALE은 박스 입수 수량",
        example = "12",
        type = "integer",
        required = true,
        implementation = Int::class,
    ) val unitsPerSale: Int = 1,
    @field:Schema(description = "판매 Organization 공개 식별자")
    val sellerOrganizationId: UUID? = null,
)
