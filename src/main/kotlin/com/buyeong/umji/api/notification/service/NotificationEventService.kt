package com.buyeong.umji.api.notification.service

import com.buyeong.umji.api.notification.model.NotificationEvent
import com.buyeong.umji.api.notification.model.NotificationEventType
import com.buyeong.umji.api.persistence.jpa.notification.service.NotificationOutboxJpaEntityService
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class NotificationEventService(private val outbox: NotificationOutboxJpaEntityService) {
    fun record(type: NotificationEventType, orderId: UUID, detail: String? = null) {
        val eventId = UUID.randomUUID()
        outbox.append(NotificationEvent(eventId, type, orderId, detail))
    }
}
