package com.buyeong.umji.api.catalog.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "ProductSummaryResponse API 데이터 모델")
data class ProductSummaryResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,

    @field:Schema(description = "Brand Name 정보", example = "예시 값", type = "string", required = true)
    val brandName: String?,

    @field:Schema(description = "판매 채널 코드", example = "WHOLESALE", type = "string", required = true)
    val channelCode: String = "WHOLESALE",

    @field:Schema(description = "채널 최저 판매 단위 가격(원). WHOLESALE은 박스당 가격", example = "12000", type = "integer", required = false)
    val startingPrice: Long? = null,

    @field:Schema(description = "시작 판매가 오퍼의 판매 단위당 입수 수량", example = "12", type = "integer", required = true, implementation = Int::class)
    val startingUnitsPerSale: Int = 1,
)