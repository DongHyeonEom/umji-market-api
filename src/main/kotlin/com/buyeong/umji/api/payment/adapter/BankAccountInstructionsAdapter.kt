package com.buyeong.umji.api.payment.adapter

import com.buyeong.umji.api.order.application.model.BankAccountInstructions
import com.buyeong.umji.api.order.application.port.out.BankAccountInstructionsPort
import org.springframework.stereotype.Component

@Component
class BankAccountInstructionsAdapter(private val properties: BankAccountProperties) : BankAccountInstructionsPort {
    override fun standard(): BankAccountInstructions = properties.standard.toInstructions()

    override fun taxInvoice(): BankAccountInstructions = properties.taxInvoice.toInstructions()

    private fun BankAccountProperty.toInstructions(): BankAccountInstructions {
        require(bankName.isNotBlank() && accountNumber.isNotBlank() && accountHolder.isNotBlank()) {
            "계좌 안내 환경변수가 설정되지 않았습니다."
        }
        return BankAccountInstructions(bankName.trim(), accountNumber.trim(), accountHolder.trim())
    }
}