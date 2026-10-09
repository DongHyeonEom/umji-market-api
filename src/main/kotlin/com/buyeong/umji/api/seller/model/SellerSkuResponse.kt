package com.buyeong.umji.api.seller.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "판매자가 오퍼를 등록할 수 있는 공용 SKU")
data class SellerSkuResponse(
    val id: UUID,
    val skuCode: String,
    val name: String,
    val productId: UUID,
    val productName: String,
    val brandName: String?,
)