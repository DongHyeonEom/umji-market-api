package com.buyeong.umji.api.order.dto

import java.util.UUID

data class TaxInvoiceBuyerDto(
    val organizationId: UUID,
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