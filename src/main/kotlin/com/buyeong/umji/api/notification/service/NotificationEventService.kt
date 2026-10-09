package com.buyeong.umji.api.notification.service

import com.buyeong.umji.api.notification.dto.NotificationEventDto
import com.buyeong.umji.api.notification.dto.NotificationEventType
import com.buyeong.umji.api.persistence.jpa.notification.service.NotificationOutboxJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class NotificationEventService(private val outbox: NotificationOutboxJpaEntityService) {
    fun record(type: NotificationEventType, orderId: UUID, detail: String? = null) {
        val eventId = UUID.randomUUID()
        outbox.append(NotificationEventDto(eventId, type, orderId, detail))
    }
}