package com.buyeong.umji.api.domain.seller.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "판매자가 오퍼를 등록할 수 있는 공용 SKU 페이지")
data class SellerSkuPageResponse(
    val items: List<SellerSkuResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)