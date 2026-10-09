package com.buyeong.umji.api.domain.order.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "세금계산서에 기재할 주문 품목과 공급가액")
data class TaxInvoiceItemResponse(
    @field:Schema(description = "품목명", example = "철물 상품 규격 A", type = "string", required = true)
    val itemName: String,

    @field:Schema(description = "주문 당시 SKU 코드", example = "SKU-001", type = "string", required = true)
    val skuCode: String,

    @field:Schema(description = "주문 수량", example = "2", format = "int32", type = "integer", required = true, implementation = Int::class)
    val quantity: Int,

    @field:Schema(description = "공급가액(원), 주문 항목 lineAmount", example = "12000", format = "int64", type = "integer", required = true, implementation = Long::class)
    val supplyAmount: Long,
)