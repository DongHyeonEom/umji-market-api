package com.buyeong.umji.api.cart.dto

import java.util.UUID

data class CartItemStateDto(val id: UUID?, val sku: SellableSkuDto, val quantity: Int)