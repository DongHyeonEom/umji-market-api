package com.buyeong.umji.api.domain.access.integration

import com.buyeong.umji.api.domain.access.model.AccessAudience
import com.buyeong.umji.api.domain.access.service.AccessContextService
import com.buyeong.umji.api.domain.auth.service.CurrentAccountService
import com.buyeong.umji.api.persistence.jpa.access.service.AccessContextJpaEntityService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import java.nio.ByteBuffer
import java.util.UUID

@SpringBootTest(properties = ["spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=none"])
@ActiveProfiles("local")
@Transactional
@Rollback
class AccessContextMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var accessContexts: AccessContextJpaEntityService

    @Autowired
    private lateinit var service: AccessContextService

    @MockitoBean
    private lateinit var currentAccounts: CurrentAccountService

    private val representativeId = UUID.randomUUID()
    private val memberId = UUID.randomUUID()
    private val organizationId = UUID.randomUUID()
    private var representativeAccountId = 0L
    private var memberAccountId = 0L
    private var organizationRowId = 0L

    @BeforeEach
    fun setUpBuyerOrganization() {
        representativeAccountId = insertAccount("access-context-representative", representativeId)
        memberAccountId = insertAccount("access-context-member", memberId)
        jdbc.update(
            """INSERT INTO organization
                (public_id, organization_type, display_name, status, representative_account_id)
                VALUES (?, 'BUSINESS', 'Access Context Test', 'ACTIVE', ?)
            """.trimIndent(),
            organizationId.toBytes(),
            representativeAccountId,
        )
        organizationRowId = jdbc.queryForObject(
            "SELECT id FROM organization WHERE public_id = ?",
            Long::class.java,
            organizationId.toBytes(),
        )!!
        jdbc.update(
            "INSERT INTO organization_member (organization_id, account_id, status) VALUES (?, ?, 'ACTIVE'), (?, ?, 'ACTIVE')",
            organizationRowId,
            representativeAccountId,
            organizationRowId,
            memberAccountId,
        )
        jdbc.update(
            "INSERT INTO organization_capability (organization_id, capability_code) VALUES (?, 'BUYER')",
            organizationRowId,
        )
        Mockito.`when`(currentAccounts.activeAccountPublicId()).thenReturn(representativeId)
    }

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
                WHERE role.code = 'SHIPPING_MANAGER' AND permission.code = 'SHIPMENT_READ'
            """.trimIndent(),
            Int::class.java,
        )
        assertThat(shippingReadPermissionCount).isEqualTo(1)

        val buyerRoleCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM organization_role_permission",
            Int::class.java,
        )
        assertThat(buyerRoleCount).isEqualTo(16)
    }

    @Test
    fun `buyer screen access recalculates when representative assignment changes`() {
        val representative = service.get(AccessAudience.BUYER)
        assertThat(representative.membershipRole).isEqualTo("REPRESENTATIVE")
        assertThat(representative.screens.map { it.screenCode })
            .contains("BUYER_GROUP_MEMBERS", "BUYER_ORDER_CREATE")

        jdbc.update(
            "UPDATE organization SET representative_account_id = ? WHERE id = ?",
            memberAccountId,
            organizationRowId,
        )

        val formerRepresentative = service.get(AccessAudience.BUYER)
        assertThat(formerRepresentative.membershipRole).isEqualTo("MEMBER")
        assertThat(formerRepresentative.organizationId).isEqualTo(organizationId)
        assertThat(formerRepresentative.screens.map { it.screenCode })
            .contains("BUYER_ORDER_CREATE")
            .doesNotContain("BUYER_GROUP_MEMBERS")
    }

    private fun insertAccount(loginId: String, publicId: UUID): Long {
        jdbc.update(
            """INSERT INTO account (public_id, login_id, password_hash, name, status)
                VALUES (?, ?, 'test-hash', ?, 'ACTIVE')
            """.trimIndent(),
            publicId.toBytes(),
            loginId,
            loginId,
        )
        return jdbc.queryForObject("SELECT id FROM account WHERE login_id = ?", Long::class.java, loginId)!!
    }

    private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16)
        .putLong(mostSignificantBits)
        .putLong(leastSignificantBits)
        .array()
}