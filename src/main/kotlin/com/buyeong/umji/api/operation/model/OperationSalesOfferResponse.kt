package com.buyeong.umji.api.operation.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "채널별 실물 SKU 판매 오퍼")
data class OperationSalesOfferResponse(
    @field:Schema(description = "오퍼 공개 UUID", format = "uuid", type = "string", required = true)
    val id: UUID,

    @field:Schema(description = "판매 채널 코드", example = "WHOLESALE", type = "string", required = true)
    val channelCode: String,

    @field:Schema(description = "공용 실물 SKU UUID", format = "uuid", type = "string", required = true)
    val skuId: UUID,

    @field:Schema(description = "채널 판매 단위 1개의 가격(원)", example = "12000", type = "integer", required = true)
    val salePrice: Long,

    @field:Schema(description = "채널 정가(원)", example = "1200", type = "integer", required = false)
    val listPrice: Long?,

    @field:Schema(description = "판매 상태", example = "ON_SALE", type = "string", required = true)
    val salesStatus: String,

    @field:Schema(description = "판매 단위당 기준 SKU 입수 수량", example = "12", type = "integer", required = true, implementation = Int::class)
    val unitsPerSale: Int = 1,
)