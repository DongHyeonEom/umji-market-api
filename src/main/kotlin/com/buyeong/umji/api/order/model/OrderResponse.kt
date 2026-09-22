package com.buyeong.umji.api.order.model

import java.time.Instant
import java.util.UUID

data class OrderResponse(
    val id: UUID,
    val orderNumber: String,
    val status: String,
    val subtotalAmount: Long,
    val totalAmount: Long,
    val orderedAt: Instant,
    val items: List<OrderItemResponse>,
)

data class OrderItemResponse(
    val id: UUID,
    val skuId: UUID,
    val productName: String,
    val skuName: String,
    val skuCode: String,
    val unitPrice: Long,
    val quantity: Int,
    val lineAmount: Long,
    val status: String,
)

data class OrderPageResponse(
    val items: List<OrderResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
