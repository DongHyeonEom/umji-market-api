package com.buyeong.umji.api.notification.service

import com.buyeong.umji.api.notification.integration.push.NotificationDeliveryService
import com.buyeong.umji.api.notification.model.NotificationDeliveryResult
import com.buyeong.umji.api.persistence.jpa.notification.NotificationOutboxWorkerJpaEntityService
import java.time.Clock
import java.time.Duration

class NotificationOutboxWorker(
    private val outbox: NotificationOutboxWorkerJpaEntityService,
    private val delivery: NotificationDeliveryService,
    private val clock: Clock,
) {
    fun dispatchBatch(): Int {
        val now = clock.instant()
        val claimed = outbox.claimBatch(now, CLAIM_LEASE, BATCH_SIZE)
        claimed.forEach { item ->
            val result = try {
                delivery.deliver(item.event)
            } catch (_: Exception) {
                NotificationDeliveryResult.RetryableFailure(UNEXPECTED_PROVIDER_ERROR)
            }

            when (result) {
                NotificationDeliveryResult.Sent -> outbox.markSent(item.event.id, clock.instant())
                is NotificationDeliveryResult.PermanentFailure ->
                    outbox.markFailed(item.event.id, clock.instant(), safeErrorCode(result.code))
                is NotificationDeliveryResult.RetryableFailure -> {
                    val errorCode = safeErrorCode(result.code)
                    if (item.attemptCount >= MAX_ATTEMPTS) {
                        outbox.markFailed(item.event.id, clock.instant(), errorCode)
                    } else {
                        outbox.reschedule(
                            item.event.id,
                            clock.instant().plus(RETRY_DELAYS[item.attemptCount - 1]),
                            errorCode,
                        )
                    }
                }
            }
        }
        return claimed.size
    }

    private fun safeErrorCode(code: String): String =
        code.trim().uppercase().replace(UNSAFE_CODE_CHARS, "_").take(MAX_ERROR_CODE_LENGTH).ifBlank { UNKNOWN_PROVIDER_ERROR }

    private companion object {
        const val BATCH_SIZE = 100
        const val MAX_ATTEMPTS = 4
        const val MAX_ERROR_CODE_LENGTH = 80
        const val UNEXPECTED_PROVIDER_ERROR = "UNEXPECTED_PROVIDER_ERROR"
        const val UNKNOWN_PROVIDER_ERROR = "UNKNOWN_PROVIDER_ERROR"
        val CLAIM_LEASE: Duration = Duration.ofMinutes(2)
        val RETRY_DELAYS = listOf(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15))
        val UNSAFE_CODE_CHARS = Regex("[^A-Z0-9_.-]")
    }
}
