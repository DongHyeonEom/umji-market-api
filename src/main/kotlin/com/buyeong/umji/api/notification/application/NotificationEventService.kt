package com.buyeong.umji.api.notification.application

import com.buyeong.umji.api.notification.application.model.NotificationEvent
import com.buyeong.umji.api.notification.application.model.NotificationEventType
import com.buyeong.umji.api.notification.application.port.`in`.NotificationEventUseCase
import com.buyeong.umji.api.notification.application.port.out.NotificationOutboxPort
import java.util.UUID

class NotificationEventService(private val outbox: NotificationOutboxPort) : NotificationEventUseCase {
    override fun record(type: NotificationEventType, orderId: UUID, detail: String?) {
        val eventId = UUID.randomUUID()
        outbox.append(NotificationEvent(eventId, type, orderId, detail))
    }
}