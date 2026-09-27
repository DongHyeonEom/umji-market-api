package com.buyeong.umji.api.payment.model

import jakarta.validation.constraints.NotBlank

data class PaymentStatusRequest(
    @field:NotBlank
    val status: String,
)
