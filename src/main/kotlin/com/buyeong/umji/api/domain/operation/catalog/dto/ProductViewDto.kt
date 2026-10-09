package com.buyeong.umji.api.domain.operation.catalog.dto

import java.util.UUID

data class ProductViewDto(
    val id: UUID,
    val categoryId: UUID,
    val brandId: UUID?,
    val name: String,
    val description: String?,
    val displayStatus: String,
    val salesStatus: String,
    val displayOrder: Int,
    val images: List<ImageViewDto> = emptyList(),
    val options: List<OptionViewDto> = emptyList(),
    val skus: List<SkuViewDto> = emptyList(),
)