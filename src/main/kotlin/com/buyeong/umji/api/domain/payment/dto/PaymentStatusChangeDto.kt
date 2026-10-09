package com.buyeong.umji.api.domain.payment.dto

import java.util.UUID

data class PaymentStatusChangeDto(
    val orderId: UUID,
    val orderStatus: String,
    val paymentStatus: String,
    val changed: Boolean,
)