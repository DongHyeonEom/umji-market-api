package com.buyeong.umji.api.domain.order.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "주문 입금에 사용할 은행 계좌 안내 정보")
data class BankAccountInstructionsResponse(
    @field:Schema(description = "입금 은행명", example = "예시 값", type = "string", required = true)
    val bankName: String,

    @field:Schema(description = "입금 계좌번호", example = "예시 값", type = "string", required = true)
    val accountNumber: String,

    @field:Schema(description = "예금주명", example = "예시 값", type = "string", required = true)
    val accountHolder: String,
)