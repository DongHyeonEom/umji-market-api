package com.buyeong.umji.api.seller.model

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

@Schema(description = "판매자가 오퍼를 등록할 수 있는 공용 SKU")
data class SellerSkuResponse(
    val id: UUID,
    val skuCode: String,
    val name: String,
    val productId: UUID,
    val productName: String,
    val brandName: String?,
)

@Schema(description = "판매자가 오퍼를 등록할 수 있는 공용 SKU 페이지")
data class SellerSkuPageResponse(
    val items: List<SellerSkuResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
