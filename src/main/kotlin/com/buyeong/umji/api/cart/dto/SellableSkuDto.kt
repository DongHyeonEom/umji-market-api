package com.buyeong.umji.api.cart.dto

import java.util.UUID

data class SellableSkuDto(
    val id: UUID,
    val salesOfferId: UUID,
    val channelCode: String,
    val code: String,
    val productName: String,
    val name: String,
    val price: Long,
    val salesStatus: String,
    val unitsPerSale: Int = 1,
    val sellerOrganizationId: UUID? = null,
)