package com.buyeong.umji.api.notification.adapter

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.notification.application.model.NotificationDevicePlatform
import com.buyeong.umji.api.notification.application.NotificationDeviceTokenService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID

@SpringBootTest(properties = ["spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=none"])
@ActiveProfiles("local")
@Transactional
@Rollback
class NotificationDeviceTokenMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var tokens: NotificationDeviceTokenService

    @Test
    fun `duplicate token registration transfers owner, keeps one row, and revocation is owner scoped`() {
        val firstAccount = createActiveAccount()
        val secondAccount = createActiveAccount()
        val token = "fcm-${UUID.randomUUID()}"
        val tokenHash = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())

        val first = tokens.register(firstAccount, NotificationDevicePlatform.ANDROID_FCM, token)
        val transferred = tokens.register(secondAccount, NotificationDevicePlatform.ANDROID_FCM, token)

        assertThat(transferred.id).isEqualTo(first.id)
        assertThat(transferred.platform).isEqualTo(NotificationDevicePlatform.ANDROID_FCM)
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification_device_token WHERE token_hash = ?", Int::class.java, tokenHash))
            .isEqualTo(1)
        assertThat(tokenOwner(first.id)).isEqualTo(secondAccount)
        assertThat(tokenStatus(first.id)).isEqualTo("ACTIVE")

        tokens.revoke(firstAccount, first.id)
        assertThat(tokenStatus(first.id)).isEqualTo("ACTIVE")

        tokens.revoke(secondAccount, first.id)
        assertThat(tokenStatus(first.id)).isEqualTo("INACTIVE")

        val reactivated = tokens.register(firstAccount, NotificationDevicePlatform.ANDROID_FCM, token)
        assertThat(reactivated.id).isEqualTo(first.id)
        assertThat(tokenOwner(first.id)).isEqualTo(firstAccount)
        assertThat(tokenStatus(first.id)).isEqualTo("ACTIVE")
    }

    @Test
    fun `inactive account cannot register a device token`() {
        val inactiveAccount = createActiveAccount(status = "INACTIVE")

        assertThatThrownBy {
            tokens.register(inactiveAccount, NotificationDevicePlatform.ANDROID_FCM, "fcm-${UUID.randomUUID()}")
        }.isInstanceOf(ItemNotFoundException::class.java)
    }

    private fun createActiveAccount(status: String = "ACTIVE"): UUID {
        val accountId = UUID.randomUUID()
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val phone = "010${suffix.filter(Char::isDigit).padEnd(8, '0').take(8)}"
        jdbc.update(
            "INSERT INTO account (public_id, login_id, password_hash, name, phone, phone_normalized, status) VALUES (?, ?, 'test-hash', 'Push token test', ?, ?, ?)",
            accountId.toBytes(),
            "push-token-${suffix.take(20)}",
            phone,
            phone,
            status,
        )
        return accountId
    }

    private fun tokenOwner(tokenId: UUID): UUID = jdbc.queryForObject(
        "SELECT account.public_id FROM notification_device_token device_token JOIN account ON account.id = device_token.account_id WHERE device_token.public_id = ?",
        { result, _ -> result.getBytes("public_id").toUuid() },
        tokenId.toBytes(),
    )!!

    private fun tokenStatus(tokenId: UUID): String = jdbc.queryForObject(
        "SELECT status FROM notification_device_token WHERE public_id = ?",
        String::class.java,
        tokenId.toBytes(),
    )!!

    private fun ByteArray.toUuid(): UUID = ByteBuffer.wrap(this).let { UUID(it.long, it.long) }

    private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16).putLong(mostSignificantBits).putLong(leastSignificantBits).array()
}