package com.buyeong.umji.api.account.dto

import java.time.Instant
import java.util.UUID

data class SharedAddressDto(
    val id: UUID,
    val recipientName: String,
    val recipientPhone: String,
    val postalCode: String,
    val address1: String,
    val address2: String?,
    val isDefault: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)