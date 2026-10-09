package com.buyeong.umji.api.domain.order.dto

import java.util.UUID

data class CancellationOrderDto(
    val id: UUID,
    val accountId: UUID,
    val orderNumber: String,
    val orderStatus: String,
    val paymentStatus: String,
    val shippingStatus: String,
    val reservationKeys: List<UUID>,
)