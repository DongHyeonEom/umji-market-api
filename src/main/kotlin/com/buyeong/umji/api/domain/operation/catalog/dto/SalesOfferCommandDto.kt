package com.buyeong.umji.api.domain.operation.catalog.dto

import java.util.UUID

data class SalesOfferCommandDto(val channelCode: String, val skuId: UUID, val salePrice: Long, val listPrice: Long?, val salesStatus: String, val unitsPerSale: Int? = null)