package com.buyeong.umji.api.persistence.jpa.auth

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest

@Service
class WebLoginAttemptJpaEntityService(private val jdbc: JdbcTemplate) {
    @Transactional(readOnly = true)
    fun isBlocked(normalizedPhone: String, remoteAddress: String): Boolean {
        val phone = hash(normalizedPhone)
        val address = hash(remoteAddress)
        val pairCount = jdbc.query(
            "SELECT failure_count FROM web_login_attempt WHERE phone_hash = ? AND remote_address_hash = ? AND window_started_at >= DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 15 MINUTE)",
            { result, _ -> result.getInt(1) },
            phone,
            address,
        ).firstOrNull() ?: 0
        val phoneCount = jdbc.queryForObject(
            "SELECT COALESCE(SUM(failure_count), 0) FROM web_login_attempt WHERE phone_hash = ? AND window_started_at >= DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 15 MINUTE)",
            Long::class.java,
            phone,
        ) ?: 0L
        return pairCount >= MAX_PAIR_FAILURES || phoneCount >= MAX_PHONE_FAILURES
    }

    @Transactional
    fun recordFailure(normalizedPhone: String, remoteAddress: String) {
        jdbc.update(
            """INSERT INTO web_login_attempt (phone_hash, remote_address_hash, window_started_at, failure_count)
                VALUES (?, ?, CURRENT_TIMESTAMP(3), 1)
                ON DUPLICATE KEY UPDATE
                    failure_count = IF(window_started_at < DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 15 MINUTE), 1, failure_count + 1),
                    window_started_at = IF(window_started_at < DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 15 MINUTE), CURRENT_TIMESTAMP(3), window_started_at)
            """.trimIndent(),
            hash(normalizedPhone),
            hash(remoteAddress),
        )
    }

    @Transactional
    fun clear(normalizedPhone: String) {
        jdbc.update(
            "DELETE FROM web_login_attempt WHERE phone_hash = ?",
            hash(normalizedPhone),
        )
    }

    @Scheduled(cron = "0 20 * * * *", zone = "UTC")
    @Transactional
    fun removeExpiredAttempts() {
        jdbc.update("DELETE FROM web_login_attempt WHERE window_started_at < DATE_SUB(CURRENT_TIMESTAMP(3), INTERVAL 1 DAY)")
    }

    private fun hash(value: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))

    private companion object {
        const val MAX_PAIR_FAILURES = 5
        const val MAX_PHONE_FAILURES = 10
    }
}