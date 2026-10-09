package com.buyeong.umji.api.cart.dto

import java.util.UUID

data class CartStateDto(val accountId: UUID, val items: List<CartItemStateDto>)