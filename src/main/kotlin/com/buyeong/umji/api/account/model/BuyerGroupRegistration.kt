package com.buyeong.umji.api.account.model

data class BuyerGroupRegistrationCommand(
    val type: String,
    val business: BusinessGroupRegistration?,
)

data class BusinessGroupRegistration(
    val businessRegistrationNumber: String,
    val businessName: String,
    val representativeName: String,
    val postalCode: String,
    val address1: String,
    val address2: String?,
    val businessIndustry: String,
    val businessItem: String,
    val email: String?,
    val confirmed: Boolean,
)
