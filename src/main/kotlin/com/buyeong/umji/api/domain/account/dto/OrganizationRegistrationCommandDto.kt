package com.buyeong.umji.api.domain.account.dto

data class OrganizationRegistrationCommandDto(
    val type: String,
    val business: BusinessGroupRegistrationDto?,
    val capability: String = "BUYER",
)