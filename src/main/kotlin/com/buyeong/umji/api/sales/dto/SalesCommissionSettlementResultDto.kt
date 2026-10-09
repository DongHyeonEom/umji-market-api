package com.buyeong.umji.api.sales.dto

import java.time.YearMonth

data class SalesCommissionSettlementResultDto(
    val month: YearMonth,
    val payableCount: Int,
    val payableAmount: Long,
)