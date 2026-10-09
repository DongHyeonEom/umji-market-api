package com.buyeong.umji.api.notification.dto

import java.util.UUID

data class NotificationEventDto(val id: UUID, val type: NotificationEventType, val orderId: UUID, val detail: String?)