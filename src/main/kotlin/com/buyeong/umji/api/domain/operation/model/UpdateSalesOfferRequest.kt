package com.buyeong.umji.api.domain.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

@Schema(description = "채널별 SKU 판매 오퍼 수정 요청")
data class UpdateSalesOfferRequest(
    @field:Min(0)
    @field:Schema(description = "채널 판매 단위 1개의 가격(원)", example = "12000", type = "integer", required = true)
    val salePrice: Long,

    @field:Min(0)
    @field:Schema(description = "채널 정가(원)", example = "1200", type = "integer", required = false)
    val listPrice: Long? = null,

    @field:NotBlank
    @field:Schema(description = "판매 상태: ON_SALE 또는 STOPPED", example = "ON_SALE", type = "string", required = true)
    val salesStatus: String,

    @field:Min(
        1,
    ) @field:Schema(
        description = "판매 단위당 기준 SKU 입수 수량. WHOLESALE은 박스 입수 수량, RETAIL은 1",
        example = "12",
        type = "integer",
        required = false,
        implementation = Int::class,
    )
    val unitsPerSale: Int? = null,
)