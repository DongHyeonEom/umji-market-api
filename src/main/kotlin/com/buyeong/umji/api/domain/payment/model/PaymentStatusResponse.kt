package com.buyeong.umji.api.domain.payment.model

import com.buyeong.umji.api.domain.payment.dto.PaymentQueueItemDto
import com.buyeong.umji.api.domain.payment.dto.PaymentQueuePageDto
import com.buyeong.umji.api.domain.payment.dto.PaymentStatusChangeDto
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "PaymentStatusResponse API 데이터 모델")
data class PaymentStatusResponse(
    @field:Schema(description = "주문 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val orderId: UUID,

    @field:Schema(description = "현재 주문 상태 코드", example = "예시 값", type = "string", required = true)
    val orderStatus: String,

    @field:Schema(description = "결제 상태 코드", example = "PENDING", type = "string", required = true)
    val paymentStatus: String,
)

fun PaymentQueuePageDto.toResponse() = OperationPaymentPageResponse(
    items.map(PaymentQueueItemDto::toResponse),
    page,
    size,
    totalElements,
    totalPages,
)

private fun PaymentQueueItemDto.toResponse() = OperationPaymentResponse(
    orderId,
    orderNumber,
    customerName,
    customerPhone,
    orderAmount,
    paymentMethod,
    paymentStatus,
    updatedAt,
)

fun PaymentStatusChangeDto.toResponse() = PaymentStatusResponse(orderId, orderStatus, paymentStatus)