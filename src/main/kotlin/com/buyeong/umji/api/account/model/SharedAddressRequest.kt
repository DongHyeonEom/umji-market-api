package com.buyeong.umji.api.account.model

import com.buyeong.umji.api.account.application.model.SharedAddressCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SharedAddressRequest(
    @field:NotBlank @field:Size(max = 100) val recipientName: String,
    @field:NotBlank @field:Size(max = 30) val recipientPhone: String,
    @field:NotBlank @field:Size(max = 20) val postalCode: String,
    @field:NotBlank @field:Size(max = 255) val address1: String,
    @field:Size(max = 255) val address2: String? = null,
    val isDefault: Boolean = false,
)

fun SharedAddressRequest.toCommand() = SharedAddressCommand(
    recipientName.trim(),
    recipientPhone.trim(),
    postalCode.trim(),
    address1.trim(),
    address2?.trim()?.takeIf(String::isNotEmpty),
    isDefault,
)