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
    val optionValueIds: Set<UUID> = emptySet(),
)

data class OperationProductImageResponse(
    val id: UUID,
    val storageKey: String,
    val altText: String?,
    val displayOrder: Int,
)

data class OperationProductOptionValueResponse(
    val id: UUID,
    val value: String,
    val displayOrder: Int,
)

data class OperationProductOptionResponse(
    val id: UUID,
    val name: String,
    val displayOrder: Int,
    val values: List<OperationProductOptionValueResponse>,
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
    val images: List<OperationProductImageResponse> = emptyList(),
    val options: List<OperationProductOptionResponse> = emptyList(),
    val skus: List<OperationProductSkuResponse> = emptyList(),
)

data class OperationProductPageResponse(
    val items: List<OperationProductResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)