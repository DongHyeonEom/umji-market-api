package com.buyeong.umji.api.order.dto

import java.util.UUID

data class OrderItemDraftDto(
    val skuId: UUID,
    val productName: String,
    val skuName: String,
    val skuCode: String,
    val unitPrice: Long,
    val quantity: Int,
    val lineAmount: Long,
    val reservationKey: UUID,
    val status: String,
    val salesOfferId: UUID = UUID(0, 0),
    val unitsPerSale: Int = 1,
)