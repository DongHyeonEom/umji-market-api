package com.buyeong.umji.api.domain.cart.dto

import java.util.UUID

data class AddCartItemCommandDto(val skuId: UUID?, val salesOfferId: UUID?, val channelCode: String?, val quantity: Int)
