package com.buyeong.umji.api.domain.operation.order.model

import com.buyeong.umji.api.domain.operation.order.dto.OperationOrderDetailDto
import java.time.Instant
import java.util.UUID

data class OperationOrderDetailResponse(
    val orderId: UUID,
    val orderNumber: String,
    val orderStatus: String,
    val orderSource: String,
    val orderedAt: Instant,
    val organizationId: UUID,
    val organizationName: String,
    val ordererName: String,
    val ordererPhoneSuffix: String?,
    val subtotalAmount: Long,
    val totalAmount: Long,
    val paymentMethod: String,
    val paymentStatus: String,
    val shipmentStatus: String,
    val taxInvoiceRequested: Boolean,
    val taxInvoiceStatus: String?,
    val items: List<OperationOrderDetailItemResponse>,
    val shipments: List<OperationOrderShipmentResponse>,
)

fun OperationOrderDetailDto.toResponse() = OperationOrderDetailResponse(
    orderId,
    orderNumber,
    orderStatus,
    orderSource,
    orderedAt,
    organizationId,
    organizationName,
    ordererName,
    ordererPhone?.filter(Char::isDigit)?.takeLast(4),
    subtotalAmount,
    totalAmount,
    paymentMethod,
    paymentStatus,
    shipmentStatus,
    taxInvoiceRequested,
    taxInvoiceStatus,
    items.map { OperationOrderDetailItemResponse(it.productName, it.skuName, it.skuCode, it.unitPrice, it.quantity, it.lineAmount) },
    shipments.map { OperationOrderShipmentResponse(it.packageId, it.brandId, it.status, it.carrierCode, it.trackingNumber, it.actualShippingCost) },
)