package com.buyeong.umji.api.domain.operation.catalog.dto

import java.util.UUID

data class SalesOfferViewDto(
    val id: UUID,
    val channelCode: String,
    val skuId: UUID,
    val salePrice: Long,
    val listPrice: Long?,
    val salesStatus: String,
    val unitsPerSale: Int = 1,
    val organizationId: UUID? = null,
)