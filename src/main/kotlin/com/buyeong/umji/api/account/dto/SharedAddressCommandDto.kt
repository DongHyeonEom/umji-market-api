package com.buyeong.umji.api.account.dto

data class SharedAddressCommandDto(
    val recipientName: String,
    val recipientPhone: String,
    val postalCode: String,
    val address1: String,
    val address2: String?,
    val isDefault: Boolean,
)