package com.buyeong.umji.api.order.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "운영자의 주문 취소 요청 처리 결과")
data class CancellationResolutionRequest(
    @field:Schema(description = "취소 요청 승인 여부", example = "true", type = "boolean", required = true, implementation = Boolean::class)
    val approved: Boolean,
)