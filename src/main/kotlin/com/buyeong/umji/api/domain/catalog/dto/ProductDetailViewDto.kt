package com.buyeong.umji.api.domain.catalog.dto

import java.util.UUID

data class ProductDetailViewDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val categoryName: String,
    val brandName: String?,
    val skus: List<ProductSkuViewDto>,
    val channelCode: String = "WHOLESALE",
)