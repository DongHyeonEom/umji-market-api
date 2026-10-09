package com.buyeong.umji.api.sales.dto

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class SalesCommissionViewDto(
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