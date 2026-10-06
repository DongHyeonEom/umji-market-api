package com.buyeong.umji.api.account.model

import java.time.Instant
import java.util.UUID

data class CustomerProfile(
    val id: UUID,
    val name: String,
    val phone: String?,
    val email: String?,
    val status: String,
)

data class SharedAddress(
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

data class SharedAddressCommand(
    val recipientName: String,
    val recipientPhone: String,
    val postalCode: String,
    val address1: String,
    val address2: String?,
    val isDefault: Boolean,
)