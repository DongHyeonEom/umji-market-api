package com.buyeong.umji.api.cart.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import java.util.UUID

@Schema(description = "AddCartItemRequest API 데이터 모델")
data class AddCartItemRequest(
    @field:Schema(
        description = "상품 옵션(SKU) 공개 식별자(UUID)",
        example = "00000000-0000-0000-0000-000000000001",
        format = "uuid",
        type = "string",
    )
    val skuId: UUID? = null,

    @field:Schema(description = "채널별 판매 오퍼 공개 식별자(UUID)", format = "uuid", type = "string")
    val salesOfferId: UUID? = null,

    @field:Schema(description = "판매 채널 코드", example = "WHOLESALE", required = true)
    val channelCode: String = "WHOLESALE",

    @field:Min(1)
    @field:Schema(description = "판매 단위 수량. WHOLESALE은 박스 수, RETAIL은 낱개 수", example = "2", format = "int32", type = "integer", required = true, implementation = Int::class)
    val quantity: Int,
)