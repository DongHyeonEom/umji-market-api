package com.buyeong.umji.api.notification.application

import com.buyeong.umji.api.notification.application.model.NotificationEvent
import com.buyeong.umji.api.notification.application.model.NotificationEventType
import com.buyeong.umji.api.notification.application.port.out.NotificationOutboxPort
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class NotificationEventService(private val outbox: NotificationOutboxPort) {
    fun record(type: NotificationEventType, orderId: UUID, detail: String? = null) {
        val eventId = UUID.randomUUID()
        outbox.append(NotificationEvent(eventId, type, orderId, detail))
    }
}
