package com.buyeong.umji.api.payment.model

import com.buyeong.umji.api.payment.model.PaymentQueueItem
import com.buyeong.umji.api.payment.model.PaymentQueuePage
import com.buyeong.umji.api.payment.model.PaymentStatusChange
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "OperationPaymentResponse API 데이터 모델")
data class OperationPaymentResponse(
    @field:Schema(description = "주문 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val orderId: UUID,
    @field:Schema(description = "사용자에게 표시하는 주문 번호", example = "UM-20261004-0001", type = "string", required = true) val orderNumber: String,
    @field:Schema(description = "Customer Name 정보", example = "예시 값", type = "string", required = true) val customerName: String,
    @field:Schema(description = "Customer Phone 정보", example = "예시 값", type = "string", required = true) val customerPhone: String?,
    @field:Schema(description = "Order Amount 정보", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val orderAmount: Long,
    @field:Schema(description = "결제 수단 코드", example = "BANK_TRANSFER", type = "string", required = true) val paymentMethod: String,
    @field:Schema(description = "결제 상태 코드", example = "PENDING", type = "string", required = true) val paymentStatus: String,
    @field:Schema(description = "마지막 수정 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true) val updatedAt: Instant,
)

@Schema(description = "운영 결제 검색 결과와 페이지 정보")
data class OperationPaymentPageResponse(
    @field:ArraySchema(
        schema = Schema(implementation = OperationPaymentResponse::class),
    ) @field:Schema(description = "현재 페이지의 항목 목록", example = "[]", type = "array", required = true) val items: List<OperationPaymentResponse>,
    @field:Schema(description = "페이지 번호(0부터 시작)", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val page: Int,
    @field:Schema(description = "페이지당 항목 수", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val size: Int,
    @field:Schema(description = "전체 검색 결과 수", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class) val totalElements: Long,
    @field:Schema(description = "전체 페이지 수", example = "1", format = "int32", type = "integer", required = true, implementation = Int::class) val totalPages: Int,
)

@Schema(description = "PaymentStatusResponse API 데이터 모델")
data class PaymentStatusResponse(
    @field:Schema(description = "주문 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true) val orderId: UUID,
    @field:Schema(description = "현재 주문 상태 코드", example = "예시 값", type = "string", required = true) val orderStatus: String,
    @field:Schema(description = "결제 상태 코드", example = "PENDING", type = "string", required = true) val paymentStatus: String,
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
