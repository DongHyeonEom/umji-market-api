package com.buyeong.umji.api.domain.operation.order.dto

import java.time.Instant
import java.util.UUID

data class OperationOrderSearchRowDto(
    val orderId: UUID,
    val orderNumber: String,
    val orderStatus: String,
    val orderedAt: Instant,
    val organizationId: UUID,
    val organizationName: String,
    val ordererName: String,
    val ordererPhone: String?,
    val totalAmount: Long,
    val paymentStatus: String,
    val shipmentStatus: String,
    val taxInvoiceRequested: Boolean,
    val taxInvoiceStatus: String?,
)