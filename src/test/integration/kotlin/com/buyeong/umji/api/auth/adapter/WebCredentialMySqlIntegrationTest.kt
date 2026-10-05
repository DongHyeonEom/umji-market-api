package com.buyeong.umji.api.auth.adapter

import com.buyeong.umji.api.auth.application.port.out.WebCredentialPort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID

@SpringBootTest(
    properties = [
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "umji.auth.totp-encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
    ],
)
@ActiveProfiles("local")
class WebCredentialMySqlIntegrationTest {
    @Autowired
    private lateinit var credentials: WebCredentialPort

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Test
    fun `admin TOTP secret is encrypted and enabling it invalidates previous sessions`() {
        val transaction = TransactionTemplate(transactionManager)
        val accountId = transaction.execute { createAdminAccount() }!!
        val internalId = jdbc.queryForObject("SELECT id FROM account WHERE public_id = ?", Long::class.java, accountId.toBytes())!!
        val secret = "JBSWY3DPEHPK3PXP"
        val tokenHash = MessageDigest.getInstance("SHA-256").digest(UUID.randomUUID().toString().toByteArray())
        try {
            transaction.executeWithoutResult {
                jdbc.update(
                    "INSERT INTO refresh_token (account_id, token_hash, expires_at) VALUES (?, ?, DATE_ADD(CURRENT_TIMESTAMP(3), INTERVAL 1 DAY))",
                    internalId,
                    tokenHash,
                )
            }

            credentials.saveTotpSecret(accountId, secret, false)
            val ciphertext = jdbc.queryForObject(
                "SELECT admin_totp_secret_encrypted FROM account WHERE public_id = ?",
                ByteArray::class.java,
                accountId.toBytes(),
            )!!
            assertThat(ciphertext.contentEquals(secret.toByteArray())).isFalse()
            assertThat(credentials.findById(accountId)?.totpSecret).isEqualTo(secret)

            credentials.saveTotpSecret(accountId, secret, true)

            assertThat(jdbc.queryForObject("SELECT admin_totp_enabled FROM account WHERE public_id = ?", Boolean::class.java, accountId.toBytes()))
                .isTrue()
            assertThat(jdbc.queryForObject("SELECT revoked_at IS NOT NULL FROM refresh_token WHERE token_hash = ?", Boolean::class.java, tokenHash))
                .isTrue()
        } finally {
            transaction.executeWithoutResult {
                jdbc.update("DELETE FROM refresh_token WHERE account_id = ?", internalId)
                jdbc.update("DELETE FROM account_role WHERE account_id = ?", internalId)
                jdbc.update("DELETE FROM account WHERE id = ?", internalId)
            }
        }
    }

    private fun createAdminAccount(): UUID {
        val publicId = UUID.randomUUID()
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val phone = "010${suffix.filter(Char::isDigit).padEnd(8, '0').take(8)}"
        jdbc.update(
            "INSERT INTO account (public_id, login_id, password_hash, name, phone, phone_normalized, status) VALUES (?, ?, NULL, 'TOTP integration', ?, ?, 'ACTIVE')",
            publicId.toBytes(),
            "totp-${suffix.take(20)}",
            phone,
            phone,
        )
        val internalId = jdbc.queryForObject("SELECT id FROM account WHERE public_id = ?", Long::class.java, publicId.toBytes())!!
        jdbc.update(
            "INSERT INTO account_role (account_id, role_id) SELECT ?, id FROM role WHERE code = 'SUPER_ADMIN'",
            internalId,
        )
        return publicId
    }

    private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16).putLong(mostSignificantBits).putLong(leastSignificantBits).array()
}