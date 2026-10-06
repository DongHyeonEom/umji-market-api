package com.buyeong.umji.api.payment.integration

import com.buyeong.umji.api.order.model.BankAccountInstructions
import com.buyeong.umji.api.payment.integration.BankAccountProperty
import org.springframework.stereotype.Component

@Component
class BankAccountInstructionsService(private val properties: BankAccountProperties) {
    fun standard(): BankAccountInstructions = properties.standard.toInstructions()

    fun taxInvoice(): BankAccountInstructions = properties.taxInvoice.toInstructions()

    private fun BankAccountProperty.toInstructions(): BankAccountInstructions {
        require(bankName.isNotBlank() && accountNumber.isNotBlank() && accountHolder.isNotBlank()) {
            "계좌 안내 환경변수가 설정되지 않았습니다."
        }
        return BankAccountInstructions(bankName.trim(), accountNumber.trim(), accountHolder.trim())
    }
}
