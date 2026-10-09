package com.buyeong.umji.api.domain.operation.account.dto

data class NewAccountDto(
    val name: String,
    val phone: String,
    val normalizedPhone: String,
    val email: String?,
    val profile: OrganizationProfileDataDto?,
    val organizationCapability: String = "BUYER",
)