package com.buyeong.umji.api.order.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "주문 취소 요청 처리 후 주문 및 요청 상태")
data class CancellationChangeResponse(
    @field:Schema(description = "주문 공개 식별자(UUID)", example = "00000000-0000-0000-0000-000000000001", format = "uuid", type = "string", required = true)
    val orderId: UUID,

    @field:Schema(description = "현재 주문 상태 코드", example = "CANCELLED", type = "string", required = true)
    val orderStatus: String,

    @field:Schema(description = "취소 요청 처리 상태 코드", example = "APPROVED", type = "string", required = true)
    val requestStatus: String,
)