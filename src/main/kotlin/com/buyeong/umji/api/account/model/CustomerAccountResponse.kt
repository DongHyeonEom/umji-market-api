package com.buyeong.umji.api.account.model

import com.buyeong.umji.api.account.application.model.CustomerProfile
import com.buyeong.umji.api.account.application.model.SharedAddress
import java.time.Instant
import java.util.UUID

data class CustomerProfileResponse(
    val id: UUID,
    val name: String,
    val phone: String?,
    val email: String?,
    val status: String,
)

data class SharedAddressResponse(
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

fun CustomerProfile.toResponse() = CustomerProfileResponse(id, name, phone, email, status)

fun SharedAddress.toResponse() = SharedAddressResponse(
    id,
    recipientName,
    recipientPhone,
    postalCode,
    address1,
    address2,
    isDefault,
    createdAt,
    updatedAt,
)