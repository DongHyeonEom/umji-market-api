package com.buyeong.umji.api.operation.audit.adapter

import com.buyeong.umji.api.operation.audit.application.model.OperationAuditEvent
import com.buyeong.umji.api.operation.audit.application.model.OperationAuditQuery
import com.buyeong.umji.api.operation.audit.application.port.`in`.OperationAuditUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant
import java.util.UUID

@SpringBootTest(properties = ["spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=none"])
@ActiveProfiles("testcontainers")
@Testcontainers(disabledWithoutDocker = true)
class OperationAuditMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var audit: OperationAuditUseCase

    @Test
    fun `flyway creates audit schema and mysql supports record search and retention deletion`() {
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
        audit.record(OperationAuditEvent(actorId, "STOCK_UPDATED", "INVENTORY", resourceId, "mysql-integration", now.minusSeconds(30)))
        audit.record(OperationAuditEvent(null, "OLD_EVENT", "PRODUCT", null, null, now.minus(Duration.ofDays(731))))

        val results = audit.search(OperationAuditQuery(actorId, "PRODUCT", now.minusSeconds(120), now, 0, 10))
        assertThat(results.totalElements).isEqualTo(1)
        assertThat(results.items.single().actorId).isEqualTo(actorId)
        assertThat(results.items.single().resourceId).isEqualTo(resourceId)
        assertThat(results.items.single().requestTraceId).isEqualTo("mysql-integration")

        val deleted = audit.purgeExpired(now.minus(Duration.ofDays(730)), 10_000)
        assertThat(deleted).isEqualTo(1)
        assertThat(audit.search(OperationAuditQuery(null, null, null, null, 0, 100)).totalElements).isEqualTo(2)
    }

    companion object {
        @Container
        @JvmStatic
        val mysql: MySQLContainer<*> =
            MySQLContainer(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("test")
                .withUsername("test")
                .withPassword("test")

        @JvmStatic
        @DynamicPropertySource
        fun mysqlProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.write.url", mysql::getJdbcUrl)
            registry.add("spring.datasource.write.username", mysql::getUsername)
            registry.add("spring.datasource.write.password", mysql::getPassword)
            registry.add("spring.datasource.read.url", mysql::getJdbcUrl)
            registry.add("spring.datasource.read.username", mysql::getUsername)
            registry.add("spring.datasource.read.password", mysql::getPassword)
        }
    }
}