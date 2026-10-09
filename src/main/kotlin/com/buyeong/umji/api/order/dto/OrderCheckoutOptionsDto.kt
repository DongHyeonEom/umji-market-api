package com.buyeong.umji.api.order.dto

data class OrderCheckoutOptionsDto(
    val defaultTaxInvoiceRequested: Boolean,
    val taxInvoiceAvailable: Boolean,
    val standardBankAccount: BankAccountInstructionsDto,
    val taxInvoiceBankAccount: BankAccountInstructionsDto,
)