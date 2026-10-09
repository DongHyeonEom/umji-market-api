package com.buyeong.umji.api.domain.catalog.dto

import com.buyeong.umji.api.domain.catalog.dto.ProductPageViewDto

data class ProductPageViewDto(
    val items: List<ProductSummaryViewDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)