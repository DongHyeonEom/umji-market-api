package com.buyeong.umji.api.order.dto

import java.util.UUID

data class CheckoutLineDto(
    val skuId: UUID,
    val skuCode: String,
    val productName: String,
    val skuName: String,
    val unitPrice: Long,
    val quantity: Int,
    val salesStatus: String,
    val salesOfferId: UUID = UUID(0, 0),
    val channelCode: String = "WHOLESALE",
    val unitsPerSale: Int = 1,
    val sellerOrganizationId: UUID? = null,
)