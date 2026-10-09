package com.buyeong.umji.api.sales.integration

import com.buyeong.umji.api.exception.InvalidRequestParameterException
import com.buyeong.umji.api.sales.model.SalesAssignmentCommand
import com.buyeong.umji.api.persistence.jpa.sales.service.OrganizationSalesAssignmentJpaEntityService
import com.buyeong.umji.api.sales.service.SalesAssignmentService
import java.nio.ByteBuffer
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
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
class SalesAssignmentMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var assignments: OrganizationSalesAssignmentJpaEntityService

    @Autowired
    private lateinit var service: SalesAssignmentService

    private val organizationPublicId = UUID.randomUUID()
    private val firstSalesPublicId = UUID.randomUUID()
    private val secondSalesPublicId = UUID.randomUUID()
    private val operatorPublicId = UUID.randomUUID()
    private val nonSalesPublicId = UUID.randomUUID()
    private var firstSalesId = 0L
    private var secondSalesId = 0L
    private var operatorId = 0L
    private var organizationId = 0L

    @BeforeEach
    fun setUp() {
        firstSalesId = insertAccount("sales-assignment-one", firstSalesPublicId)
        secondSalesId = insertAccount("sales-assignment-two", secondSalesPublicId)
        operatorId = insertAccount("sales-assignment-operator", operatorPublicId)
        insertAccount("sales-assignment-non-sales", nonSalesPublicId)
        val salesRoleId = jdbc.queryForObject("SELECT id FROM role WHERE code = 'SALES_MANAGER'", Long::class.java)!!
        jdbc.update("INSERT INTO account_role (account_id, role_id, granted_by) VALUES (?, ?, ?), (?, ?, ?)", firstSalesId, salesRoleId, operatorId, secondSalesId, salesRoleId, operatorId)
        jdbc.update(
            """INSERT INTO organization (public_id, organization_type, display_name, status)
                VALUES (?, 'BUSINESS', 'Sales Assignment Test', 'ACTIVE')""".trimIndent(),
            organizationPublicId.toBytes(),
        )
        organizationId = jdbc.queryForObject(
            "SELECT id FROM organization WHERE public_id = ?",
            Long::class.java,
            organizationPublicId.toBytes(),
        )!!
        jdbc.update(
            "INSERT INTO organization_capability (organization_id, capability_code) VALUES (?, 'BUYER')",
            organizationId,
        )
    }

    @Test
    fun `reassignment closes previous period and preserves owner and rate history`() {
        val first = assignments.assign(
            organizationPublicId,
            SalesAssignmentCommand(firstSalesPublicId, 30, "INITIAL_ASSIGNMENT", operatorPublicId),
        ).single()
        val history = assignments.assign(
            organizationPublicId,
            SalesAssignmentCommand(secondSalesPublicId, null, "RATE_NOT_SET", operatorPublicId),
        )

        assertThat(history).hasSize(2)
        assertThat(history[0].salesAccountId).isEqualTo(secondSalesPublicId)
        assertThat(history[0].commissionRateBps).isNull()
        assertThat(history[0].validUntil).isNull()
        assertThat(history[1].id).isEqualTo(first.id)
        assertThat(history[1].salesAccountId).isEqualTo(firstSalesPublicId)
        assertThat(history[1].commissionRateBps).isEqualTo(30)
        assertThat(history[1].validUntil).isNotNull()
        assertThat(history[0].validFrom).isAfterOrEqualTo(history[1].validUntil)
        assertThat(jdbc.queryForObject(
            """SELECT COUNT(*) FROM permission WHERE code = 'SALES_GROUP_ASSIGN'""",
            Int::class.java,
        )).isEqualTo(1)
        assertThat(jdbc.queryForObject(
            """SELECT COUNT(*) FROM role_permission rp
                JOIN role ON role.id = rp.role_id JOIN permission ON permission.id = rp.permission_id
                WHERE role.code IN ('ADMIN', 'SUPER_ADMIN') AND permission.code = 'SALES_GROUP_ASSIGN'""",
            Int::class.java,
        )).isEqualTo(2)
    }

    @Test
    fun `repeating same active assignment is idempotent`() {
        val command = SalesAssignmentCommand(firstSalesPublicId, 30, "INITIAL_ASSIGNMENT", operatorPublicId)

        assignments.assign(organizationPublicId, command)
        val repeated = assignments.assign(organizationPublicId, command)

        assertThat(repeated).hasSize(1)
        assertThat(repeated.single().validUntil).isNull()
    }

    @Test
    fun `inactive sales role is rejected and invalid rate is rejected`() {
        assertThatThrownBy {
            service.assign(
                organizationPublicId,
                SalesAssignmentCommand(nonSalesPublicId, 30, "INVALID_ROLE", operatorPublicId),
            )
        }.isInstanceOf(InvalidRequestParameterException::class.java)

        assertThatThrownBy {
            service.assign(
                organizationPublicId,
                SalesAssignmentCommand(firstSalesPublicId, 10_001, "INVALID_RATE", operatorPublicId),
            )
        }.isInstanceOf(InvalidRequestParameterException::class.java)
    }

    private fun insertAccount(loginId: String, publicId: UUID): Long {
        jdbc.update(
            """INSERT INTO account (public_id, login_id, password_hash, name, status)
                VALUES (?, ?, 'test-hash', ?, 'ACTIVE')""".trimIndent(),
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
