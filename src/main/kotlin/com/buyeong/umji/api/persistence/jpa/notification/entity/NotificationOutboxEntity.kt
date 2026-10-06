package com.buyeong.umji.api.persistence.jpa.notification.entity

import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notification_outbox")
class NotificationOutboxEntity : DomainPublicEntity() {
    @Column(name = "event_type", nullable = false, length = 80)
    lateinit var eventType: String

    @Column(name = "event_detail", length = 80)
    var eventDetail: String? = null

    @Column(name = "order_public_id", nullable = false)
    lateinit var orderPublicId: UUID

    @Column(nullable = false, length = 20)
    var status: String = PENDING

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0

    @Column(name = "next_attempt_at", nullable = false)
    var nextAttemptAt: Instant = Instant.now()

    @Column(name = "last_error_code", length = 80)
    var lastErrorCode: String? = null

    @Column(name = "sent_at")
    var sentAt: Instant? = null

    fun setEventId(eventId: UUID) {
        publicId = eventId
    }

    private companion object {
        const val PENDING = "PENDING"
    }
}
