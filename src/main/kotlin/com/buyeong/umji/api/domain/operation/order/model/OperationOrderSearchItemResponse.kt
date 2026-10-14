package com.buyeong.umji.api.domain.operation.order.model

import com.buyeong.umji.api.domain.operation.order.dto.OperationOrderSearchRowDto
import java.time.Instant
import java.util.UUID

data class OperationOrderSearchItemResponse(
    val orderId: UUID,
    val orderNumber: String,
    val orderStatus: String,
    val orderedAt: Instant,
    val organizationId: UUID,
    val organizationName: String,
    val ordererName: String,
    val ordererPhoneSuffix: String?,
    val totalAmount: Long,
    val paymentStatus: String,
    val shipmentStatus: String,
    val taxInvoiceRequested: Boolean,
    val taxInvoiceStatus: String?,
)

fun OperationOrderSearchRowDto.toResponse() = OperationOrderSearchItemResponse(
    orderId,
    orderNumber,
    orderStatus,
    orderedAt,
    organizationId,
    organizationName,
    ordererName,
    ordererPhone?.filter(Char::isDigit)?.takeLast(4),
    totalAmount,
    paymentStatus,
    shipmentStatus,
    taxInvoiceRequested,
    taxInvoiceStatus,
)