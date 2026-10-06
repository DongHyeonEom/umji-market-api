package com.buyeong.umji.api.payment.integration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("umji.payment.bank-account")
class BankAccountProperties {
    var standard = BankAccountProperty()
    var taxInvoice = BankAccountProperty()
}

class BankAccountProperty {
    var bankName: String = ""
    var accountNumber: String = ""
    var accountHolder: String = ""
}