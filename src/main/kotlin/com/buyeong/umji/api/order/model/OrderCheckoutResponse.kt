package com.buyeong.umji.api.order.model

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "판매 Organization별로 분리 생성된 주문 목록")
data class OrderCheckoutResponse(
    @field:ArraySchema(schema = Schema(implementation = OrderResponse::class))
    val orders: List<OrderResponse>,
)