package com.buyeong.umji.api.domain.order.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "주문 상품의 SKU·주문 당시 가격·수량 정보")
data class OrderItemResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "상품 옵션(SKU) 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val skuId: UUID,

    @field:Schema(description = "상품명", example = "예시 값", type = "string", required = true)
    val productName: String,

    @field:Schema(description = "상품 옵션명", example = "예시 값", type = "string", required = true)
    val skuName: String,

    @field:Schema(description = "상품 옵션 코드", example = "예시 값", type = "string", required = true)
    val skuCode: String,

    @field:Schema(description = "판매 단위 1개의 주문 당시 가격(원). WHOLESALE은 박스당 가격", example = "12000", format = "int64", type = "integer", required = true, implementation = Long::class)
    val unitPrice: Long,

    @field:Schema(description = "판매 단위 주문 수량. WHOLESALE은 박스 수, RETAIL은 낱개 수", example = "2", format = "int32", type = "integer", required = true, implementation = Int::class)
    val quantity: Int,

    @field:Schema(description = "수량을 반영한 항목 금액(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class)
    val lineAmount: Long,

    @field:Schema(description = "현재 상태 코드", example = "ACTIVE", type = "string", required = true)
    val status: String,

    @field:Schema(description = "채널별 판매 오퍼 공개 UUID")
    val salesOfferId: UUID? = null,

    @field:Schema(
        description = "판매 단위당 기준 SKU 입수 수량. WHOLESALE은 박스 입수 수량",
        example = "12",
        type = "integer",
        required = true,
        implementation = Int::class,
    )
    val unitsPerSale: Int = 1,
)