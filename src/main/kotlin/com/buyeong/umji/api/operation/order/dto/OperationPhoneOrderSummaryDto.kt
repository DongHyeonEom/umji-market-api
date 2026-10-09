package com.buyeong.umji.api.operation.order.dto

import java.util.UUID

data class OperationPhoneOrderSummaryDto(
    val id: UUID,
    val orderNumber: String,
    val status: String,
    val totalAmount: Long,
    val orderedAt: java.time.Instant,
    val sellerOrganizationId: UUID?,
)