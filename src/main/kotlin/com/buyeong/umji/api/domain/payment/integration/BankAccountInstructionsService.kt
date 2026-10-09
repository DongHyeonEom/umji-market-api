package com.buyeong.umji.api.domain.payment.integration

import com.buyeong.umji.api.domain.order.dto.BankAccountInstructionsDto
import com.buyeong.umji.api.domain.payment.integration.BankAccountProperty
import org.springframework.stereotype.Component

@Component
class BankAccountInstructionsService(private val properties: BankAccountProperties) {
    fun standard(): BankAccountInstructionsDto = properties.standard.toInstructions()

    fun taxInvoice(): BankAccountInstructionsDto = properties.taxInvoice.toInstructions()

    private fun BankAccountProperty.toInstructions(): BankAccountInstructionsDto {
        require(bankName.isNotBlank() && accountNumber.isNotBlank() && accountHolder.isNotBlank()) {
            "계좌 안내 환경변수가 설정되지 않았습니다."
        }
        return BankAccountInstructionsDto(bankName.trim(), accountNumber.trim(), accountHolder.trim())
    }
}