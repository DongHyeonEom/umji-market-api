package com.buyeong.umji.api.domain.order.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "세금계산서 설정과 주문 유형별 입금 계좌 정보")
data class OrderCheckoutOptionsResponse(
    @field:Schema(description = "Organization 기본 세금계산서 발행 설정", example = "true", type = "boolean", required = true, implementation = Boolean::class)
    val defaultTaxInvoiceRequested: Boolean,

    @field:Schema(
        description = "공급자와 그룹 공급받는자 필수 정보가 완성되어 발행 요청할 수 있는지 여부",
        example = "true",
        type = "boolean",
        required = true,
        implementation = Boolean::class,
    )
    val taxInvoiceAvailable: Boolean,

    @field:Schema(description = "Standard Bank Account 정보", example = "예시 값", type = "object", required = true)
    val standardBankAccount: BankAccountInstructionsResponse,

    @field:Schema(description = "Tax Invoice Bank Account 정보", example = "예시 값", type = "object", required = true)
    val taxInvoiceBankAccount: BankAccountInstructionsResponse,
)