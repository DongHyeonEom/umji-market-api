package com.buyeong.umji.api.catalog.dto

import java.util.UUID

data class ProductSummaryViewDto(
    val id: UUID,
    val name: String,
    val brandName: String?,
    val channelCode: String = "WHOLESALE",
    val startingPrice: Long? = null,
    val startingUnitsPerSale: Int = 1,
)