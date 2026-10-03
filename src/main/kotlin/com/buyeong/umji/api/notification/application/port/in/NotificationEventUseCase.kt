package com.buyeong.umji.api.notification.application.port.`in`

import com.buyeong.umji.api.notification.application.model.NotificationEventType
import java.util.UUID

interface NotificationEventUseCase {
    fun record(type: NotificationEventType, orderId: UUID, detail: String? = null)
}

object NoOpNotificationEventUseCase : NotificationEventUseCase {
    override fun record(type: NotificationEventType, orderId: UUID, detail: String?) = Unit
}