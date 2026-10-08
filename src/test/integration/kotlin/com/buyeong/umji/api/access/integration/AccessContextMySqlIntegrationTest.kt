package com.buyeong.umji.api.access.integration

import com.buyeong.umji.api.persistence.jpa.access.service.AccessContextJpaEntityService
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
class AccessContextMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var accessContexts: AccessContextJpaEntityService

    @Test
    fun `flyway v46 installs screen mappings buyer role permissions and shipping read permission`() {
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '46' AND success = TRUE",
                Int::class.java,
            ),
        ).isEqualTo(1)

        val adminScreens = accessContexts.findScreens("ADMIN").associateBy { it.screenCode }
        val buyerScreens = accessContexts.findScreens("BUYER").associateBy { it.screenCode }
        assertThat(adminScreens["ADMIN_SHIPMENT_LIST"]?.requiredPermissions).containsExactly("SHIPMENT_READ")
        assertThat(adminScreens["ADMIN_SHIPMENT_DETAIL"]?.requiredPermissions).containsExactly("SHIPMENT_READ")
        assertThat(buyerScreens["BUYER_GROUP_ONBOARDING"]?.requiredPermissions).containsExactly("BUYER_GROUP_ONBOARDING_READ")
        assertThat(buyerScreens["BUYER_GROUP_MEMBERS"]?.requiredPermissions)
            .containsExactlyInAnyOrder("BUYER_GROUP_INVITE", "BUYER_GROUP_JOIN_REQUEST_MANAGE")

        val shippingReadPermissionCount = jdbc.queryForObject(
            """SELECT COUNT(*) FROM role_permission
                JOIN role ON role.id = role_permission.role_id
                JOIN permission ON permission.id = role_permission.permission_id
                WHERE role.code = 'SHIPPING_MANAGER' AND permission.code = 'SHIPMENT_READ'""".trimIndent(),
            Int::class.java,
        )
        assertThat(shippingReadPermissionCount).isEqualTo(1)

        val buyerRoleCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM organization_role_permission",
            Int::class.java,
        )
        assertThat(buyerRoleCount).isEqualTo(16)
    }
}
