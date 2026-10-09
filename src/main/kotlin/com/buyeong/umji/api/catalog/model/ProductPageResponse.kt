package com.buyeong.umji.api.catalog.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "상품 검색 결과와 페이지 정보")
data class ProductPageResponse(
    @field:ArraySchema(
        schema = Schema(implementation = ProductSummaryResponse::class),
    ) @field:Schema(description = "현재 페이지의 항목 목록", example = "[]", type = "array", required = true)
    val items: List<ProductSummaryResponse>,

    @field:Schema(description = "페이지 번호(0부터 시작)", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class)
    val page: Int,

    @field:Schema(description = "페이지당 항목 수", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class)
    val size: Int,

    @field:Schema(description = "전체 검색 결과 수", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class)
    val totalElements: Long,

    @field:Schema(description = "전체 페이지 수", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class)
    val totalPages: Int,
)