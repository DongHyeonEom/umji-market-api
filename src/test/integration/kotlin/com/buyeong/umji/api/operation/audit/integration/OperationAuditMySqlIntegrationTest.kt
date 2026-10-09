package com.buyeong.umji.api.operation.audit.integration

import com.buyeong.umji.api.operation.audit.model.OperationAuditEvent
import com.buyeong.umji.api.operation.audit.model.OperationAuditQuery
import com.buyeong.umji.api.operation.audit.service.OperationAuditService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

@SpringBootTest(properties = ["spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=none"])
@ActiveProfiles("local")
@Transactional
@Rollback
class OperationAuditMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var audit: OperationAuditService

    @Test
    fun `mysql persists order resource audit events and supports order filtering`() {
        val actorId = UUID.randomUUID()
        val orderId = UUID.randomUUID()
        val occurredAt = Instant.now()
        audit.record(
            OperationAuditEvent(
                actorId,
                "POST /api/operation/orders/phone-orders",
                "ORDER",
                orderId,
                "mysql-order-audit",
                occurredAt,
            ),
        )

        val result = audit.search(OperationAuditQuery(actorId, "ORDER", occurredAt.minusSeconds(1), occurredAt.plusSeconds(1), 0, 10))

        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.items.single().resourceId).isEqualTo(orderId)
        assertThat(result.items.single().action).isEqualTo("POST /api/operation/orders/phone-orders")
        assertThat(result.items.single().requestTraceId).isEqualTo("mysql-order-audit")
    }

    @Test
    fun `flyway creates audit schema and mysql supports record search and retention deletion`() {
        val flywayV11Count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '11' AND success = TRUE",
            Int::class.java,
        )
        assertThat(flywayV11Count).isEqualTo(1)

        val flywayV12Count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '12' AND success = TRUE",
            Int::class.java,
        )
        assertThat(flywayV12Count).isEqualTo(1)
        val missingPaymentRows = jdbc.queryForObject(
            "SELECT COUNT(*) FROM purchase_order o LEFT JOIN order_payment p ON p.order_id = o.id WHERE p.id IS NULL",
            Int::class.java,
        )
        assertThat(missingPaymentRows).isZero()

        val unpaidOrderWithoutPaymentCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM purchase_order o LEFT JOIN order_payment p ON p.order_id = o.id WHERE p.id IS NULL",
            Int::class.java,
        )
        assertThat(unpaidOrderWithoutPaymentCount).isZero()

        val tableCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'operation_audit_log'",
            Int::class.java,
        )
        assertThat(tableCount).isEqualTo(1)

        val superAdminPermissionCount = jdbc.queryForObject(
            """SELECT COUNT(*) FROM role_permission rp
                JOIN role r ON r.id = rp.role_id
                JOIN permission p ON p.id = rp.permission_id
                WHERE r.code = 'SUPER_ADMIN' AND p.code = 'ADMIN_AUDIT_READ'
            """.trimIndent(),
            Int::class.java,
        )
        val adminPermissionCount = jdbc.queryForObject(
            """SELECT COUNT(*) FROM role_permission rp
                JOIN role r ON r.id = rp.role_id
                JOIN permission p ON p.id = rp.permission_id
                WHERE r.code = 'ADMIN' AND p.code = 'ADMIN_AUDIT_READ'
            """.trimIndent(),
            Int::class.java,
        )
        assertThat(superAdminPermissionCount).isEqualTo(1)
        assertThat(adminPermissionCount).isZero()

        val actorId = UUID.randomUUID()
        val resourceId = UUID.randomUUID()
        val now = Instant.now()
        audit.record(OperationAuditEvent(actorId, "PRODUCT_UPDATED", "PRODUCT", resourceId, "mysql-integration", now.minusSeconds(60)))
        audit.record(OperationAuditEvent(actorId, "POST /api/operation/orders/phone-orders", "ORDER", resourceId, "mysql-order-integration", now.minusSeconds(45)))
        audit.record(OperationAuditEvent(actorId, "STOCK_UPDATED", "INVENTORY", resourceId, "mysql-integration", now.minusSeconds(30)))
        audit.record(OperationAuditEvent(actorId, "OLD_EVENT", "PRODUCT", null, "mysql-integration-expired", now.minus(Duration.ofDays(731))))

        val results = audit.search(OperationAuditQuery(actorId, "PRODUCT", now.minusSeconds(120), now, 0, 10))
        assertThat(results.totalElements).isEqualTo(1)
        assertThat(results.items.single().actorId).isEqualTo(actorId)
        assertThat(results.items.single().resourceId).isEqualTo(resourceId)
        assertThat(results.items.single().requestTraceId).isEqualTo("mysql-integration")

        val orderAudit = audit.search(OperationAuditQuery(actorId, "ORDER", now.minusSeconds(120), now, 0, 10))
        assertThat(orderAudit.totalElements).isEqualTo(1)
        assertThat(orderAudit.items.single().action).isEqualTo("POST /api/operation/orders/phone-orders")
        assertThat(orderAudit.items.single().resourceId).isEqualTo(resourceId)
        assertThat(orderAudit.items.single().requestTraceId).isEqualTo("mysql-order-integration")

        val deleted = audit.purgeExpired(now.minus(Duration.ofDays(730)), 10_000)
        assertThat(deleted).isGreaterThanOrEqualTo(1)
        val deletedEvent = audit.search(
            OperationAuditQuery(
                actorId,
                "PRODUCT",
                now.minus(Duration.ofDays(732)),
                now.minus(Duration.ofDays(730)),
                0,
                10,
            ),
        )
        assertThat(deletedEvent.totalElements).isZero()
        assertThat(audit.search(OperationAuditQuery(actorId, null, null, null, 0, 10)).totalElements).isEqualTo(3)
    }
}