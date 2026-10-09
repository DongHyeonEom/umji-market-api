package com.buyeong.umji.api.operation.account.dto

data class OrganizationProfileDataDto(
    val businessName: String,
    val businessRegistrationNumber: String?,
    val representativeName: String?,
    val businessPhone: String?,
    val postalCode: String?,
    val address1: String?,
    val address2: String?,
    val status: String = "COMPLETED",
)