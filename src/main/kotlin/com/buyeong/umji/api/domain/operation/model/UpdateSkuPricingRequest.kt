package com.buyeong.umji.api.domain.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

@Schema(description = "SKU 소매·도매 가격과 상품별 할인 기준 수정 요청")
data class UpdateSkuPricingRequest(
    @field:Min(0)
    @field:Schema(description = "판매 단위당 소매 가격(원)", example = "15000", required = true)
    val retailSalePrice: Long,

    @field:Min(0)
    @field:Schema(description = "판매 단위당 도매 가격(원)", example = "12000", required = true)
    val wholesaleSalePrice: Long,

    @field:Min(1) @field:Max(5)
    @field:Schema(description = "상품의 SKU 수량을 합산해 도매 가격을 적용할 기준 박스 수(1~5)", example = "3", required = true)
    val wholesaleMinimumQuantity: Int,

    @field:NotBlank @field:Pattern(regexp = "ON_SALE|STOPPED")
    @field:Schema(description = "판매 상태: ON_SALE 또는 STOPPED", example = "ON_SALE", required = true)
    val salesStatus: String,

    @field:Min(1)
    @field:Schema(description = "판매 단위당 기준 SKU 입수 수량", example = "12", required = true)
    val unitsPerSale: Int,
)
