package com.buyeong.umji.api.cart.model

import java.util.UUID

data class CartResponse(
    val items: List<CartItemResponse>,
)

data class CartItemResponse(
    val id: UUID,
    val skuId: UUID,
    val skuCode: String,
    val productName: String,
    val skuName: String,
    val quantity: Int,
    val currentSalePrice: Long,
    val salesStatus: String,
)
