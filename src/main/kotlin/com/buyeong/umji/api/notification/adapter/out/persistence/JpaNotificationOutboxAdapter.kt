package com.buyeong.umji.api.notification.adapter.out.persistence

import com.buyeong.umji.api.notification.application.model.NotificationEvent
import com.buyeong.umji.api.notification.application.port.out.NotificationOutboxPort
import com.buyeong.umji.api.persistence.jpa.notification.NotificationOutboxEntity
import com.buyeong.umji.api.persistence.jpa.notification.NotificationOutboxRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class JpaNotificationOutboxAdapter(private val outbox: NotificationOutboxRepository) : NotificationOutboxPort {
    @Transactional
    override fun append(event: NotificationEvent) {
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