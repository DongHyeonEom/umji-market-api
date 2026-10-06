package com.buyeong.umji.api.persistence.jpa.notification

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.notification.model.NotificationDevicePlatform
import com.buyeong.umji.api.notification.model.NotificationDeviceRecipient
import com.buyeong.umji.api.notification.model.NotificationDeviceTokenRegistration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Service
class NotificationDeviceTokenJpaEntityService(private val jdbc: JdbcTemplate) {
    @Transactional
    fun register(
        accountId: UUID,
        platform: NotificationDevicePlatform,
        token: String,
        tokenHash: ByteArray,
        registeredAt: Instant,
    ): NotificationDeviceTokenRegistration {
        val timestamp = Timestamp.from(registeredAt)
        val inserted = jdbc.update(
            """INSERT INTO notification_device_token
                (public_id, account_id, platform, token_value, token_hash, status, last_registered_at, created_at, updated_at)
                SELECT ?, account.id, ?, ?, ?, 'ACTIVE', ?, ?, ?
                FROM account
                WHERE account.public_id = ? AND account.status = 'ACTIVE'
                ON DUPLICATE KEY UPDATE
                    account_id = VALUES(account_id), platform = VALUES(platform), token_value = VALUES(token_value),
                    status = 'ACTIVE', last_registered_at = VALUES(last_registered_at), updated_at = VALUES(updated_at)
            """.trimIndent(),
            UUID.randomUUID().toBytes(),
            platform.name,
            token,
            tokenHash,
            timestamp,
            timestamp,
            timestamp,
            accountId.toBytes(),
        )
        if (inserted == 0) throw ItemNotFoundException("활성 계정을 찾을 수 없습니다.")

        return jdbc.queryForObject(
            """SELECT public_id, platform, last_registered_at
                FROM notification_device_token
                WHERE token_hash = ?
            """.trimIndent(),
            { result, _ ->
                NotificationDeviceTokenRegistration(
                    result.getBytes("public_id").toUuid(),
                    NotificationDevicePlatform.valueOf(result.getString("platform")),
                    result.getTimestamp("last_registered_at").toInstant(),
                )
            },
            tokenHash,
        ) ?: throw ItemNotFoundException("푸시 token을 찾을 수 없습니다.")
    }

    @Transactional
    fun revoke(accountId: UUID, tokenId: UUID) {
        jdbc.update(
            """UPDATE notification_device_token device_token
                JOIN account ON account.id = device_token.account_id
                SET device_token.status = 'INACTIVE', device_token.updated_at = CURRENT_TIMESTAMP(3)
                WHERE device_token.public_id = ? AND account.public_id = ?
            """.trimIndent(),
            tokenId.toBytes(),
            accountId.toBytes(),
        )
    }

    @Transactional(readOnly = true)
    fun activeRecipientsForOrder(orderId: UUID): List<NotificationDeviceRecipient> =
        jdbc.query(
            """SELECT device_token.public_id, device_token.platform, device_token.token_value
                FROM purchase_order
                JOIN notification_device_token device_token ON device_token.account_id = purchase_order.account_id
                WHERE purchase_order.public_id = ? AND device_token.status = 'ACTIVE'
                ORDER BY device_token.id
            """.trimIndent(),
            { result, _ ->
                NotificationDeviceRecipient(
                    result.getBytes("public_id").toUuid(),
                    NotificationDevicePlatform.valueOf(result.getString("platform")),
                    result.getString("token_value"),
                )
            },
            orderId.toBytes(),
        )

    @Transactional
    fun deactivate(tokenId: UUID) {
        jdbc.update(
            """UPDATE notification_device_token
                SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP(3)
                WHERE public_id = ? AND status = 'ACTIVE'
            """.trimIndent(),
            tokenId.toBytes(),
        )
    }

    private fun ByteArray.toUuid(): UUID = ByteBuffer.wrap(this).let { UUID(it.long, it.long) }

    private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16).putLong(mostSignificantBits).putLong(leastSignificantBits).array()
}