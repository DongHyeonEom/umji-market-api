package com.buyeong.umji.api.cart.dto

import java.util.UUID

data class AddCartItemCommandDto(val skuId: UUID?, val salesOfferId: UUID?, val channelCode: String = "WHOLESALE", val quantity: Int)