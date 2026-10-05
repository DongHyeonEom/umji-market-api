package com.buyeong.umji.api.auth.adapter

import com.buyeong.umji.api.auth.application.port.out.WebLoginAttemptPort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.util.UUID

@SpringBootTest(properties = ["spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=none"])
@ActiveProfiles("local")
@Transactional
@Rollback
class WebLoginAttemptMySqlIntegrationTest {
    @Autowired
    private lateinit var attempts: WebLoginAttemptPort

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Test
    fun `same phone and address is blocked after five failures and phone-wide limit spans addresses`() {
        val phone = "010${UUID.randomUUID().toString().filter(Char::isDigit).padEnd(8, '0').take(8)}"
        val firstAddress = "192.0.2.11-${UUID.randomUUID()}"
        val secondAddress = "192.0.2.12-${UUID.randomUUID()}"

        repeat(4) { attempts.recordFailure(phone, firstAddress) }
        assertThat(attempts.isBlocked(phone, firstAddress)).isFalse()
        attempts.recordFailure(phone, firstAddress)
        assertThat(attempts.isBlocked(phone, firstAddress)).isTrue()
        assertThat(attempts.isBlocked(phone, secondAddress)).isFalse()

        repeat(5) { attempts.recordFailure(phone, secondAddress) }
        assertThat(attempts.isBlocked(phone, secondAddress)).isTrue()

        val phoneHash = MessageDigest.getInstance("SHA-256").digest(phone.toByteArray())
        val addressHash = MessageDigest.getInstance("SHA-256").digest(firstAddress.toByteArray())
        val hashedRowCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM web_login_attempt WHERE phone_hash = ? AND remote_address_hash = ?",
            Int::class.java,
            phoneHash,
            addressHash,
        )
        assertThat(hashedRowCount).isEqualTo(1)
        val plaintextRowCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM web_login_attempt WHERE phone_hash = ? AND remote_address_hash = ?",
            Int::class.java,
            phone.toByteArray(),
            firstAddress.toByteArray(),
        )
        assertThat(plaintextRowCount).isEqualTo(0)

        attempts.clear(phone)
        assertThat(attempts.isBlocked(phone, secondAddress)).isFalse()
    }
}