package com.buyeong.umji.api.catalog.application.model

import java.util.UUID

data class CategoryView(val id: UUID, val name: String, val path: String, val depth: Int)
data class ProductSummaryView(val id: UUID, val name: String, val brandName: String?)
data class ProductSkuView(val id: UUID, val code: String, val name: String, val salePrice: Long, val listPrice: Long?)
data class ProductDetailView(
    val id: UUID,
    val name: String,
    val description: String?,
    val categoryName: String,
    val brandName: String?,
    val skus: List<ProductSkuView>,
)
data class ProductPageView(
    val items: List<ProductSummaryView>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
