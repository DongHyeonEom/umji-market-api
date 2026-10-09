package com.buyeong.umji.api.access.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "허용된 화면 식별자와 route key")
data class AccessScreenResponse(
    @field:Schema(description = "DB에 등록된 안정적인 screen code", example = "BUYER_ORDERS", required = true)
    val screenCode: String,

    @field:Schema(description = "React 화면 registry와 연결할 route key", example = "BUYER_ORDERS", required = true)
    val routeKey: String,
)