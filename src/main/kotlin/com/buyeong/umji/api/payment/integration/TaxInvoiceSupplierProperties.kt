package com.buyeong.umji.api.payment.integration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("umji.tax-invoice.supplier")
class TaxInvoiceSupplierProperties {
    var businessRegistrationNumber: String = ""
    var businessName: String = ""
    var representativeName: String = ""
    var businessAddress: String = ""
    var businessIndustry: String = ""
    var businessItem: String = ""
    var email: String = ""
}