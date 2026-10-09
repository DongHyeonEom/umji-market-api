package com.buyeong.umji.api.payment.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "OperationPaymentResponse API 데이터 모델")
data class OperationPaymentResponse(
    @field:Schema(description = "주문 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val orderId: UUID,

    @field:Schema(description = "사용자에게 표시하는 주문 번호", example = "UM-20261004-0001", type = "string", required = true)
    val orderNumber: String,

    @field:Schema(description = "Customer Name 정보", example = "예시 값", type = "string", required = true)
    val customerName: String,

    @field:Schema(description = "Customer Phone 정보", example = "예시 값", type = "string", required = true)
    val customerPhone: String?,

    @field:Schema(description = "Order Amount 정보", example = "1", format = "int64", type = "integer", required = true, implementation = Long::class)
    val orderAmount: Long,

    @field:Schema(description = "결제 수단 코드", example = "BANK_TRANSFER", type = "string", required = true)
    val paymentMethod: String,

    @field:Schema(description = "결제 상태 코드", example = "PENDING", type = "string", required = true)
    val paymentStatus: String,

    @field:Schema(description = "마지막 수정 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true)
    val updatedAt: Instant,
)