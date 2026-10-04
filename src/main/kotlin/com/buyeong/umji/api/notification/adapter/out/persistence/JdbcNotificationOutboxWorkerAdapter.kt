package com.buyeong.umji.api.notification.adapter.out.persistence

import com.buyeong.umji.api.notification.application.model.NotificationEvent
import com.buyeong.umji.api.notification.application.model.NotificationEventType
import com.buyeong.umji.api.notification.application.port.out.ClaimedNotification
import com.buyeong.umji.api.notification.application.port.out.NotificationOutboxWorkerPort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Component
class JdbcNotificationOutboxWorkerAdapter(private val jdbc: JdbcTemplate) : NotificationOutboxWorkerPort {
    @Transactional
    override fun claimBatch(now: Instant, lease: Duration, limit: Int): List<ClaimedNotification> {
        require(!lease.isNegative && !lease.isZero)
        require(limit in 1..MAX_BATCH_SIZE)
        val rows = jdbc.query(
            """SELECT id, public_id, event_type, event_detail, order_public_id, attempt_count
                FROM notification_outbox
                WHERE (status = 'PENDING' AND next_attempt_at <= ?)
                   OR (status = 'PROCESSING' AND lease_expires_at <= ?)
                ORDER BY id
                LIMIT ? FOR UPDATE SKIP LOCKED
            """.trimIndent(),
            { result, _ ->
                ClaimedRow(
                    result.getLong("id"),
                    result.getBytes("public_id").toUuid(),
                    result.getString("event_type"),
                    result.getString("event_detail"),
                    result.getBytes("order_public_id").toUuid(),
                    result.getInt("attempt_count"),
                )
            },
            Timestamp.from(now),
            Timestamp.from(now),
            limit,
        )
        if (rows.isEmpty()) return emptyList()

        val supportedRows = rows.mapNotNull { row ->
            val type = runCatching { NotificationEventType.valueOf(row.eventType) }.getOrNull()
            if (type == null) {
                jdbc.update(
                    """UPDATE notification_outbox
                        SET status = 'FAILED', last_error_code = 'UNKNOWN_EVENT_TYPE', lease_expires_at = NULL, updated_at = ?
                        WHERE id = ?
                    """.trimIndent(),
                    Timestamp.from(now),
                    row.id,
                )
                null
            } else {
                row to type
            }
        }
        val leaseExpiry = Timestamp.from(now.plus(lease))
        supportedRows.forEach { (row, _) ->
            jdbc.update(
                """UPDATE notification_outbox
                    SET status = 'PROCESSING', attempt_count = attempt_count + 1, lease_expires_at = ?, updated_at = ?
                    WHERE id = ?
                """.trimIndent(),
                leaseExpiry,
                Timestamp.from(now),
                row.id,
            )
        }
        return supportedRows.map { (row, type) ->
            ClaimedNotification(
                NotificationEvent(
                    row.eventId,
                    type,
                    row.orderId,
                    row.detail,
                ),
                row.attemptCount + 1,
            )
        }
    }

    @Transactional
    override fun markSent(eventId: UUID, sentAt: Instant) {
        jdbc.update(
            """UPDATE notification_outbox
                SET status = 'SENT', sent_at = ?, last_error_code = NULL, lease_expires_at = NULL, updated_at = ?
                WHERE public_id = ? AND status = 'PROCESSING'
            """.trimIndent(),
            Timestamp.from(sentAt),
            Timestamp.from(sentAt),
            eventId.toBytes(),
        )
    }

    @Transactional
    override fun reschedule(eventId: UUID, nextAttemptAt: Instant, errorCode: String) {
        jdbc.update(
            """UPDATE notification_outbox
                SET status = 'PENDING', next_attempt_at = ?, last_error_code = ?, lease_expires_at = NULL, updated_at = ?
                WHERE public_id = ? AND status = 'PROCESSING'
            """.trimIndent(),
            Timestamp.from(nextAttemptAt),
            errorCode,
            Timestamp.from(Instant.now()),
            eventId.toBytes(),
        )
    }

    @Transactional
    override fun markFailed(eventId: UUID, failedAt: Instant, errorCode: String) {
        jdbc.update(
            """UPDATE notification_outbox
                SET status = 'FAILED', last_error_code = ?, lease_expires_at = NULL, updated_at = ?
                WHERE public_id = ? AND status = 'PROCESSING'
            """.trimIndent(),
            errorCode,
            Timestamp.from(failedAt),
            eventId.toBytes(),
        )
    }

    private data class ClaimedRow(
        val id: Long,
        val eventId: UUID,
        val eventType: String,
        val detail: String?,
        val orderId: UUID,
        val attemptCount: Int,
    )

    private fun ByteArray.toUuid(): UUID = ByteBuffer.wrap(this).let { UUID(it.long, it.long) }

    private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16).putLong(mostSignificantBits).putLong(leastSignificantBits).array()

    private companion object {
        const val MAX_BATCH_SIZE = 500
    }
}