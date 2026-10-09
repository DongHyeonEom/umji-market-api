package com.buyeong.umji.api.seller.dto

import java.util.UUID

data class SellerSkuResponseDto(
    val id: UUID,
    val skuCode: String,
    val name: String,
    val productId: UUID,
    val productName: String,
    val brandName: String?,
)