package com.buyeong.umji.api.domain.notification.dto

enum class NotificationEventType {
    ORDER_CREATED,
    ORDER_CANCELLED,
    PAYMENT_STATUS_CHANGED,
    TAX_INVOICE_ISSUED,
    SHIPMENT_PREPARING,
    SHIPMENT_IN_TRANSIT,
    SHIPMENT_DELIVERED,
}