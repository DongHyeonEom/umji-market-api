package com.buyeong.umji.api.payment.dto

import java.util.UUID

data class PaymentRecordDto(
    val orderId: UUID,
    val orderStatus: String,
    val paymentStatus: String,
)