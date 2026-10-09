package com.buyeong.umji.api.domain.catalog.dto

import java.util.UUID

data class ProductSkuViewDto(
    val id: UUID,
    val code: String,
    val name: String,
    val salePrice: Long,
    val listPrice: Long?,
    val salesOfferId: UUID? = null,
    val unitsPerSale: Int = 1,
    val sellerOrganizationId: UUID? = null,
)