package com.buyeong.umji.api.sales.model

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class SalesCommissionView(
    val id: UUID,
    val orderId: UUID,
    val salesAccountId: UUID?,
    val rateBps: Int?,
    val basisAmount: Long,
    val commissionAmount: Long,
    val status: String,
    val settlementMonth: LocalDate?,
    val qualifiedAt: Instant?,
    val paidAt: Instant?,
    val createdAt: Instant,
)

data class SalesCommissionPage(
    val items: List<SalesCommissionView>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class SalesCommissionSettlementResult(
    val month: YearMonth,
    val payableCount: Int,
    val payableAmount: Long,
)
