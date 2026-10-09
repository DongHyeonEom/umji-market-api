package com.buyeong.umji.api.sales.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "주문 인센티브 페이지와 페이지 정보")
data class SalesCommissionPageResponse(
    @field:Schema(description = "현재 페이지의 주문 인센티브 목록", example = "[]", type = "array", required = true)
    val items: List<SalesCommissionItemResponse>,

    @field:Schema(description = "페이지 번호(0부터 시작)", example = "0", type = "integer", required = true)
    val page: Int,

    @field:Schema(description = "페이지당 항목 수", example = "20", type = "integer", required = true)
    val size: Int,

    @field:Schema(description = "전체 인센티브 건수", example = "1", type = "integer", required = true)
    val totalElements: Long,

    @field:Schema(description = "전체 페이지 수", example = "1", type = "integer", required = true)
    val totalPages: Int,
)