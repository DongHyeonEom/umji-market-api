package com.buyeong.umji.api.operation.catalog.dto

import java.util.UUID

data class ProductCommandDto(
    val categoryId: UUID,
    val brandId: UUID?,
    val name: String,
    val description: String?,
    val displayStatus: String,
    val salesStatus: String,
    val displayOrder: Int,
    val images: List<ImageCommandDto> = emptyList(),
    val options: List<OptionCommandDto> = emptyList(),
    val skus: List<SkuCommandDto> = emptyList(),
)