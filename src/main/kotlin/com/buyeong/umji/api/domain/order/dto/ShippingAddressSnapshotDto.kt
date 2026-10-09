package com.buyeong.umji.api.domain.order.dto

data class ShippingAddressSnapshotDto(
    val recipientName: String,
    val recipientPhone: String,
    val postalCode: String,
    val address1: String,
    val address2: String?,
)