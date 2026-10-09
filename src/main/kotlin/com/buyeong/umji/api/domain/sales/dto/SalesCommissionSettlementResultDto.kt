package com.buyeong.umji.api.domain.sales.dto

import java.time.YearMonth

data class SalesCommissionSettlementResultDto(
    val month: YearMonth,
    val payableCount: Int,
    val payableAmount: Long,
)