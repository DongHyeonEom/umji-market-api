package com.buyeong.umji.api.notification.application.port.out

import com.buyeong.umji.api.notification.application.model.NotificationEvent
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class ClaimedNotification(val event: NotificationEvent, val attemptCount: Int)

interface NotificationOutboxWorkerPort {
    fun claimBatch(now: Instant, lease: Duration, limit: Int): List<ClaimedNotification>

    fun markSent(eventId: UUID, sentAt: Instant)

    fun reschedule(eventId: UUID, nextAttemptAt: Instant, errorCode: String)

    fun markFailed(eventId: UUID, failedAt: Instant, errorCode: String)
}