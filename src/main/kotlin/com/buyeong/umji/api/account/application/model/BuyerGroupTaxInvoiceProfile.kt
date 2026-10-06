package com.buyeong.umji.api.account.application.model

import java.util.UUID

data class BuyerGroupTaxInvoiceProfile(
    val buyerGroupId: UUID,
    val groupType: String,
    val businessRegistrationNumber: String?,
    val businessName: String?,
    val representativeName: String?,
    val postalCode: String?,
    val address1: String?,
    val address2: String?,
    val businessIndustry: String?,
    val businessItem: String?,
    val email: String?,
    val complete: Boolean,
)

data class BuyerGroupTaxInvoiceProfileCommand(
    val businessRegistrationNumber: String?,
    val businessName: String,
    val representativeName: String?,
    val postalCode: String?,
    val address1: String?,
    val address2: String?,
    val businessIndustry: String?,
    val businessItem: String?,
    val email: String?,
)
