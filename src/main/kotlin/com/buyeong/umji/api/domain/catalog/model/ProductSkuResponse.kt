package com.buyeong.umji.api.domain.catalog.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "ProductSkuResponse API 데이터 모델")
data class ProductSkuResponse(
    @field:Schema(description = "리소스 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "상품 옵션 코드", example = "예시 값", type = "string", required = true)
    val skuCode: String,

    @field:Schema(description = "이름 또는 표시 이름", example = "홍길동", type = "string", required = true)
    val name: String,

    @field:Schema(description = "판매 단위 1개의 가격(원). WHOLESALE은 박스당 가격", example = "12000", format = "int64", type = "integer", required = true, implementation = Long::class)
    val salePrice: Long,

    @field:Schema(description = "정가(원)", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class)
    val listPrice: Long?,

    @field:Schema(description = "채널별 판매 오퍼 공개 UUID", format = "uuid", type = "string", required = true)
    val salesOfferId: UUID? = null,

    @field:Schema(
        description = "판매 단위당 기준 SKU 입수 수량. WHOLESALE은 박스 입수 수량",
        example = "12",
        type = "integer",
        required = true,
        implementation = Int::class,
    )
    val unitsPerSale: Int = 1,

    @field:Schema(description = "판매 Organization 공개 식별자", format = "uuid", type = "string")
    val sellerOrganizationId: UUID? = null,
)