package com.buyeong.umji.api.payment.integration

import com.buyeong.umji.api.order.dto.TaxInvoiceSupplierDto
import org.springframework.stereotype.Component

@Component
class TaxInvoiceSupplierService(private val properties: TaxInvoiceSupplierProperties) {
    fun supplier(): TaxInvoiceSupplierDto? {
        val values = listOf(
            properties.businessRegistrationNumber,
            properties.businessName,
            properties.representativeName,
            properties.businessAddress,
            properties.businessIndustry,
            properties.businessItem,
            properties.email,
        ).map(String::trim)
        if (values.any(String::isBlank)) return null
        return TaxInvoiceSupplierDto(
            values[0],
            values[1],
            values[2],
            values[3],
            values[4],
            values[5],
            values[6],
        )
    }
}