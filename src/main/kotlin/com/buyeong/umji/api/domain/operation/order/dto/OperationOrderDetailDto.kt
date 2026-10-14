package com.buyeong.umji.api.domain.operation.order.dto

import java.time.Instant
import java.util.UUID

data class OperationOrderDetailDto(
    val orderId: UUID,
    val orderNumber: String,
    val orderStatus: String,
    val orderSource: String,
    val orderedAt: Instant,
    val organizationId: UUID,
    val organizationName: String,
    val ordererName: String,
    val ordererPhone: String?,
    val subtotalAmount: Long,
    val totalAmount: Long,
    val paymentMethod: String,
    val paymentStatus: String,
    val shipmentStatus: String,
    val taxInvoiceRequested: Boolean,
    val taxInvoiceStatus: String?,
    val items: List<OperationOrderDetailItemDto>,
    val shipments: List<OperationOrderShipmentDto>,
)