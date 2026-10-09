package com.buyeong.umji.api.domain.cart.dto

import java.util.UUID

data class CartStateDto(val accountId: UUID, val items: List<CartItemStateDto>)