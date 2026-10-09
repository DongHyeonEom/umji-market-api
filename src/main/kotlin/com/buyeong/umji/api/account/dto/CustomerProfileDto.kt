package com.buyeong.umji.api.account.dto

import java.util.UUID

data class CustomerProfileDto(
    val id: UUID,
    val name: String,
    val phone: String?,
    val email: String?,
    val status: String,
)