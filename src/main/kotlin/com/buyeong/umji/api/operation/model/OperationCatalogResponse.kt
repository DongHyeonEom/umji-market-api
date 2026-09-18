package com.buyeong.umji.api.operation.model

import java.util.UUID

data class OperationCatalogResourceResponse(
    val id: UUID,
)

data class OperationCategoryResponse(
    val id: UUID,
    val parentId: UUID?,
    val name: String,
    val path: String,
    val depth: Int,
    val displayOrder: Int,
    val displayStatus: String,
)

data class OperationBrandResponse(
    val id: UUID,
    val name: String,
    val displayStatus: String,
)

data class OperationProductSkuResponse(
    val id: UUID,
    val skuCode: String,
    val name: String,
    val salePrice: Long,
    val listPrice: Long?,
    val salesStatus: String,
)

data class OperationProductResponse(
    val id: UUID,
    val categoryId: UUID,
    val brandId: UUID?,
    val name: String,
    val description: String?,
    val displayStatus: String,
    val salesStatus: String,
    val displayOrder: Int,
    val skus: List<OperationProductSkuResponse> = emptyList(),
)

data class OperationProductPageResponse(
    val items: List<OperationProductResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
