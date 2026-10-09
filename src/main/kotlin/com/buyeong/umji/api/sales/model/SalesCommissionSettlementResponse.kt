package com.buyeong.umji.api.sales.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "월말 인센티브 정산 처리 결과")
data class SalesCommissionSettlementResponse(
    @field:Schema(description = "정산 대상 월", example = "2026-09", type = "string", required = true)
    val month: String,

    @field:Schema(description = "이번 실행에서 PAYABLE로 전환한 건수", example = "12", type = "integer", required = true)
    val payableCount: Int,

    @field:Schema(description = "이번 실행에서 PAYABLE로 전환한 인센티브 합계(KRW)", example = "36000", type = "integer", format = "int64", required = true)
    val payableAmount: Long,
)