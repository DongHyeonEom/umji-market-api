package com.buyeong.umji.api.domain.cart.dto

import java.util.UUID

data class CartItemViewDto(
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
    val sellerOrganizationId: UUID? = null,
)