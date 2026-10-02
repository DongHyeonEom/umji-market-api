package com.buyeong.umji.api.payment.model

import com.buyeong.umji.api.payment.application.model.PaymentQueueItem
import com.buyeong.umji.api.payment.application.model.PaymentQueuePage
import com.buyeong.umji.api.payment.application.model.PaymentStatusChange
import java.time.Instant
import java.util.UUID

data class OperationPaymentResponse(
    val orderId: UUID,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String?,
    val orderAmount: Long,
    val paymentMethod: String,
    val paymentStatus: String,
    val updatedAt: Instant,
)

data class OperationPaymentPageResponse(
    val items: List<OperationPaymentResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class PaymentStatusResponse(
    val orderId: UUID,
    val orderStatus: String,
    val paymentStatus: String,
)

fun PaymentQueuePage.toResponse() = OperationPaymentPageResponse(
    items.map(PaymentQueueItem::toResponse),
    page,
    size,
    totalElements,
    totalPages,
)

private fun PaymentQueueItem.toResponse() = OperationPaymentResponse(
    orderId,
    orderNumber,
    customerName,
    customerPhone,
    orderAmount,
    paymentMethod,
    paymentStatus,
    updatedAt,
)

fun PaymentStatusChange.toResponse() = PaymentStatusResponse(orderId, orderStatus, paymentStatus)