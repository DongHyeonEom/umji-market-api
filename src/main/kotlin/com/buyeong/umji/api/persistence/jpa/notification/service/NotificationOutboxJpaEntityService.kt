package com.buyeong.umji.api.persistence.jpa.notification.service

import com.buyeong.umji.api.notification.dto.NotificationEventDto
import com.buyeong.umji.api.persistence.jpa.notification.entity.NotificationOutboxEntity
import com.buyeong.umji.api.persistence.jpa.notification.repository.NotificationOutboxRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationOutboxJpaEntityService(private val outbox: NotificationOutboxRepository) {
    @Transactional
    fun append(event: NotificationEventDto) {
        outbox.saveAndFlush(
            NotificationOutboxEntity().apply {
                setEventId(event.id)
                eventType = event.type.name
                eventDetail = event.detail
                orderPublicId = event.orderId
            },
        )
    }
}