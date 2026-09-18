package com.buyeong.umji.api.catalog.model

import java.util.UUID

data class CategoryResponse(
    val id: UUID,
    val name: String,
    val path: String,
    val depth: Int,
)

data class ProductSummaryResponse(
    val id: UUID,
    val name: String,
    val brandName: String?,
)

data class ProductSkuResponse(
    val id: UUID,
    val skuCode: String,
    val name: String,
    val salePrice: Long,
    val listPrice: Long?,
)

data class ProductDetailResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val categoryName: String,
    val brandName: String?,
    val skus: List<ProductSkuResponse>,
)

data class ProductPageResponse(
    val items: List<ProductSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
