package com.buyeong.umji.api.order.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "운영자 처리를 기다리는 주문 취소 요청")
data class CancellationQueueItemResponse(
    @field:Schema(description = "주문 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val orderId: UUID,

    @field:Schema(description = "사용자에게 표시하는 주문 번호", example = "UM-20261004-0001", type = "string", required = true)
    val orderNumber: String,

    @field:Schema(description = "주문자 계정 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val accountId: UUID,

    @field:Schema(description = "취소 요청 시각(ISO-8601)", example = "2026-10-04T09:00:00Z", format = "date-time", type = "string", required = true)
    val requestedAt: Instant,
)