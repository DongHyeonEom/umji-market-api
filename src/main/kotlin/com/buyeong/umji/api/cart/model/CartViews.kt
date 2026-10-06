package com.buyeong.umji.api.cart.model

import java.util.UUID

data class SellableSku(
    val id: UUID,
    val salesOfferId: UUID,
    val channelCode: String,
    val code: String,
    val productName: String,
    val name: String,
    val price: Long,
    val salesStatus: String,
    val unitsPerSale: Int = 1,
)
data class CartItemState(val id: UUID?, val sku: SellableSku, val quantity: Int)
data class CartState(val accountId: UUID, val items: List<CartItemState>)
data class CartItemView(
    val id: UUID,
    val skuId: UUID,
    val skuCode: String,
    val productName: String,
    val skuName: String,
    val quantity: Int,
    val unitPrice: Long,
    val salesStatus: String,
    val salesOfferId: UUID,
    val channelCode: String,
    val unitsPerSale: Int = 1,
)
data class CartView(val items: List<CartItemView>)
data class AddCartItemCommand(val skuId: UUID?, val salesOfferId: UUID?, val channelCode: String = "WHOLESALE", val quantity: Int)
data class UpdateCartItemCommand(val quantity: Int)
