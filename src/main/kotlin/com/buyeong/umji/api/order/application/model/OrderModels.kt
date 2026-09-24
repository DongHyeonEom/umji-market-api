package com.buyeong.umji.api.order.application.model

import java.time.Instant
import java.util.UUID

data class CheckoutLine(
    val skuId: UUID,
    val skuCode: String,
    val productName: String,
    val skuName: String,
    val unitPrice: Long,
    val quantity: Int,
    val salesStatus: String,
)

data class OrderDraft(
    val accountId: UUID,
    val status: String,
    val orderedAt: Instant,
    val subtotalAmount: Long,
    val totalAmount: Long,
    val items: List<OrderItemDraft>,
)

data class OrderItemDraft(
    val skuId: UUID,
    val productName: String,
    val skuName: String,
    val skuCode: String,
    val unitPrice: Long,
    val quantity: Int,
    val lineAmount: Long,
    val reservationKey: UUID,
    val status: String,
)

data class OrderView(
    val id: UUID,
    val orderNumber: String,
    val status: String,
    val subtotalAmount: Long,
    val totalAmount: Long,
    val orderedAt: Instant,
    val items: List<OrderItemView>,
)

data class OrderItemView(
    val id: UUID,
    val skuId: UUID,
    val reservationKey: UUID,
    val productName: String,
    val skuName: String,
    val skuCode: String,
    val unitPrice: Long,
    val quantity: Int,
    val lineAmount: Long,
    val status: String,
)

data class OrderPage(
    val items: List<OrderView>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)