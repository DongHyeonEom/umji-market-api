package com.buyeong.umji.api.domain.notification.integration

import com.buyeong.umji.api.domain.notification.model.NotificationEventType
import com.buyeong.umji.api.domain.notification.service.NotificationEventService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@SpringBootTest(properties = ["spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=none"])
@ActiveProfiles("local")
class NotificationOutboxMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var notificationEvents: NotificationEventService

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Test
    fun `flyway v24 stores outbox event in the same transaction as business state and rolls both back`() {
        val migrationCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '24' AND success = TRUE",
            Int::class.java,
        )
        assertThat(migrationCount).isEqualTo(1)

        val transaction = TransactionTemplate(transactionManager)
        val orderId = transaction.execute { createOrder() }!!

        assertThatThrownBy {
            transaction.executeWithoutResult {
                jdbc.update("UPDATE purchase_order SET status = 'PAID' WHERE public_id = ?", orderId.toBytes())
                notificationEvents.record(NotificationEventType.PAYMENT_STATUS_CHANGED, orderId, "PAYMENT_CONFIRMED")
                error("force transaction rollback")
            }
        }.isInstanceOf(IllegalStateException::class.java)

        transaction.executeWithoutResult {
            assertThat(orderStatus(orderId)).isEqualTo("PENDING_PAYMENT")
            assertThat(outboxCount(orderId)).isZero()
        }

        transaction.executeWithoutResult {
            jdbc.update("UPDATE purchase_order SET status = 'PAID' WHERE public_id = ?", orderId.toBytes())
            notificationEvents.record(NotificationEventType.PAYMENT_STATUS_CHANGED, orderId, "PAYMENT_CONFIRMED")
        }

        transaction.executeWithoutResult {
            assertThat(orderStatus(orderId)).isEqualTo("PAID")
            assertThat(outboxCount(orderId)).isEqualTo(1)
            assertThat(
                jdbc.queryForObject(
                    "SELECT event_detail FROM notification_outbox WHERE order_public_id = ?",
                    String::class.java,
                    orderId.toBytes(),
                ),
            ).isEqualTo("PAYMENT_CONFIRMED")
            assertThat(
                jdbc.queryForObject(
                    "SELECT status FROM notification_outbox WHERE order_public_id = ?",
                    String::class.java,
                    orderId.toBytes(),
                ),
            ).isEqualTo("PENDING")
        }
    }

    private fun createOrder(): UUID {
        val accountId = UUID.randomUUID()
        val phone = "010${UUID.randomUUID().toString().filter(Char::isDigit).padEnd(8, '0').take(8)}"
        jdbc.update(
            "INSERT INTO account (public_id, login_id, password_hash, name, phone, phone_normalized, status) VALUES (?, ?, 'test-hash', 'Outbox test', ?, ?, 'ACTIVE')",
            accountId.toBytes(),
            "outbox-${UUID.randomUUID()}",
            phone,
            phone,
        )
        val internalAccountId = jdbc.queryForObject(
            "SELECT id FROM account WHERE public_id = ?",
            Long::class.java,
            accountId.toBytes(),
        )!!
        val organizationId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO organization (public_id, organization_type, display_name, status) VALUES (?, 'INDIVIDUAL', 'Outbox test', 'ACTIVE')",
            organizationId.toBytes(),
        )
        val internalGroupId = jdbc.queryForObject(
            "SELECT id FROM organization WHERE public_id = ?",
            Long::class.java,
            organizationId.toBytes(),
        )!!
        jdbc.update(
            "INSERT INTO organization_member (organization_id, account_id, status) VALUES (?, ?, 'ACTIVE')",
            internalGroupId,
            internalAccountId,
        )
        val orderId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO purchase_order (public_id, order_number, account_id, organization_id, status, subtotal_amount, total_amount, ordered_at) VALUES (?, ?, ?, ?, 'PENDING_PAYMENT', 1000, 1000, ?)",
            orderId.toBytes(),
            "OUT-${UUID.randomUUID()}",
            internalAccountId,
            internalGroupId,
            Timestamp.from(Instant.now()),
        )
        return orderId
    }

    private fun orderStatus(orderId: UUID): String = jdbc.queryForObject(
        "SELECT status FROM purchase_order WHERE public_id = ?",
        String::class.java,
        orderId.toBytes(),
    )!!

    private fun outboxCount(orderId: UUID): Int = jdbc.queryForObject(
        "SELECT COUNT(*) FROM notification_outbox WHERE order_public_id = ?",
        Int::class.java,
        orderId.toBytes(),
    )!!

    private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16).putLong(mostSignificantBits).putLong(leastSignificantBits).array()
}