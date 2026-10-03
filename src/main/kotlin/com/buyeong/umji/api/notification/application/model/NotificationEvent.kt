package com.buyeong.umji.api.notification.application.model

import java.util.UUID

enum class NotificationEventType {
    ORDER_CREATED,
    ORDER_CANCELLED,
    PAYMENT_STATUS_CHANGED,
    SHIPMENT_PREPARING,
    SHIPMENT_IN_TRANSIT,
    SHIPMENT_DELIVERED,
}

data class NotificationEvent(val id: UUID, val type: NotificationEventType, val orderId: UUID, val detail: String?)