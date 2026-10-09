package com.buyeong.umji.api.catalog.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "ProductDetailResponse API 데이터 모델")
data class ProductDetailResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,

    @field:Schema(description = "상세 설명", example = "예시 값", type = "string", required = true)
    val description: String?,

    @field:Schema(description = "Category Name 정보", example = "예시 값", type = "string", required = true)
    val categoryName: String,

    @field:Schema(description = "Brand Name 정보", example = "예시 값", type = "string", required = true)
    val brandName: String?,

    @field:ArraySchema(
        schema = Schema(implementation = ProductSkuResponse::class),
    ) @field:Schema(description = "상품 SKU 목록", example = "[]", type = "array", required = true)
    val skus: List<ProductSkuResponse>,

    @field:Schema(description = "판매 채널 코드", example = "WHOLESALE", type = "string", required = true)
    val channelCode: String = "WHOLESALE",
)