package com.buyeong.umji.api.domain.seller.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "판매 Organization에 귀속된 판매 오퍼")
data class SellerSalesOfferResponse(
    val id: UUID,
    val organizationId: UUID,
    val channelCode: String,
    val skuId: UUID,
    val salePrice: Long,
    val listPrice: Long?,
    val salesStatus: String,
    val unitsPerSale: Int,
)