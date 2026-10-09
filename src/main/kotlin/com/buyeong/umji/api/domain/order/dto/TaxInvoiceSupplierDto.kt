package com.buyeong.umji.api.domain.order.dto

data class TaxInvoiceSupplierDto(
    val businessRegistrationNumber: String,
    val businessName: String,
    val representativeName: String,
    val businessAddress: String,
    val businessIndustry: String,
    val businessItem: String,
    val email: String,
)