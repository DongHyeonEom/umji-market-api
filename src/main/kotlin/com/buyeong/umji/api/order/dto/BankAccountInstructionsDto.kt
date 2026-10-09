package com.buyeong.umji.api.order.dto

data class BankAccountInstructionsDto(
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
)