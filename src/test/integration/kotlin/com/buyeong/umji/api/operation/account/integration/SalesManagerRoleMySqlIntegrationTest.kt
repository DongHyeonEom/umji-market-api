package com.buyeong.umji.api.operation.account.integration

import com.buyeong.umji.api.persistence.jpa.account.service.OperationAccountJpaEntityService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(properties = ["spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=none"])
@ActiveProfiles("local")
@Transactional
@Rollback
class SalesManagerRoleMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var operationAccounts: OperationAccountJpaEntityService

    @Test
    fun `sales manager receives only sales permissions and is available for role management`() {
        assertThat(operationAccounts.managedRoles().map { it.code })
            .contains("SALES_MANAGER", "SHIPPING_MANAGER")

        val salesPermissions = jdbc.queryForList(
            """SELECT permission.code FROM role_permission
                JOIN role ON role.id = role_permission.role_id
                JOIN permission ON permission.id = role_permission.permission_id
                WHERE role.code = 'SALES_MANAGER' ORDER BY permission.code""",
            String::class.java,
        )
        assertThat(salesPermissions).containsExactly("SALES_COMMISSION_READ", "SALES_GROUP_CREATE", "SALES_GROUP_READ")

        val salesWriteAccess = jdbc.queryForObject(
            """SELECT COUNT(*) FROM role_permission rp
                JOIN role r ON r.id = rp.role_id
                JOIN permission p ON p.id = rp.permission_id
                WHERE r.code = 'SALES_MANAGER'
                  AND p.code IN ('ADMIN_ACCOUNT_MANAGE', 'ORDER_WRITE', 'SHIPMENT_WRITE', 'SALES_GROUP_ASSIGN', 'SALES_COMMISSION_SETTLE')""",
            Int::class.java,
        )
        assertThat(salesWriteAccess).isZero()

        val administratorSalesPermissions = jdbc.queryForObject(
            """SELECT COUNT(*) FROM role_permission rp
                JOIN role r ON r.id = rp.role_id
                JOIN permission p ON p.id = rp.permission_id
                WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
                  AND p.code IN ('SALES_GROUP_CREATE', 'SALES_GROUP_READ', 'SALES_COMMISSION_READ')""",
            Int::class.java,
        )
        assertThat(administratorSalesPermissions).isEqualTo(6)
    }
}
