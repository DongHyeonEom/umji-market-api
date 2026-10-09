package com.buyeong.umji.api.sales.integration

import com.buyeong.umji.api.sales.service.SalesCommissionService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

@SpringBootTest(properties = ["spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=none"])
@ActiveProfiles("local")
@Transactional
@Rollback
class SalesCommissionMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var commissions: SalesCommissionService

    private val buyerId = UUID.randomUUID()
    private val salesOneId = UUID.randomUUID()
    private val salesTwoId = UUID.randomUUID()
    private val adminId = UUID.randomUUID()
    private val organizationId = UUID.randomUUID()
    private val firstOrderId = UUID.randomUUID()
    private val secondOrderId = UUID.randomUUID()
    private var buyerAccountInternalId = 0L
    private var salesOneInternalId = 0L
    private var salesTwoInternalId = 0L
    private var adminInternalId = 0L
    private var organizationInternalId = 0L

    @BeforeEach
    fun setUp() {
        buyerAccountInternalId = insertAccount("commission-buyer", buyerId)
        salesOneInternalId = insertAccount("commission-sales-one", salesOneId)
        salesTwoInternalId = insertAccount("commission-sales-two", salesTwoId)
        adminInternalId = insertAccount("commission-admin", adminId)
        val salesRole = jdbc.queryForObject("SELECT id FROM role WHERE code = 'SALES_MANAGER'", Long::class.java)!!
        jdbc.update(
            "INSERT INTO account_role (account_id, role_id, granted_by) VALUES (?, ?, ?), (?, ?, ?)",
            salesOneInternalId,
            salesRole,
            adminInternalId,
            salesTwoInternalId,
            salesRole,
            adminInternalId,
        )
        jdbc.update(
            """INSERT INTO organization (public_id, organization_type, display_name, status, representative_account_id)
                VALUES (?, 'BUSINESS', 'Commission Test Organization', 'ACTIVE', ?)
            """.trimIndent(),
            organizationId.toBytes(),
            buyerAccountInternalId,
        )
        organizationInternalId = jdbc.queryForObject(
            "SELECT id FROM organization WHERE public_id = ?",
            Long::class.java,
            organizationId.toBytes(),
        )!!
        jdbc.update(
            "INSERT INTO organization_member (organization_id, account_id, status) VALUES (?, ?, 'ACTIVE')",
            organizationInternalId,
            buyerAccountInternalId,
        )
        jdbc.update("INSERT INTO organization_capability (organization_id, capability_code) VALUES (?, 'BUYER')", organizationInternalId)

        val month = closedMonth()
        val firstValidFrom = month.minusMonths(1).atEndOfMonth().atStartOfDay(KST).toInstant()
        val reassignmentAt = month.plusMonths(1).atDay(1).atStartOfDay(KST).toInstant()
        insertAssignment(salesOneInternalId, 30, firstValidFrom, reassignmentAt)
        insertAssignment(salesTwoInternalId, 50, reassignmentAt, null)

        val firstOrderAt = month.atDay(5).atStartOfDay(KST).toInstant()
        val paidAt = month.atDay(10).atTime(12, 0).atZone(KST).toInstant()
        val deliveredAt = month.atDay(12).atTime(12, 0).atZone(KST).toInstant()
        insertOrder(firstOrderId, "UMJ-COMMISSION-000001", firstOrderAt, "PAID", paidAt, deliveredAt)
        insertOrder(
            secondOrderId,
            "UMJ-COMMISSION-000002",
            reassignmentAt.plusSeconds(1),
            "PENDING_PAYMENT",
            null,
            null,
        )
    }

    @Test
    fun `order snapshots preserve assignment rate and settle eligible month idempotently`() {
        val month = closedMonth()
        val firstOrderedAt = month.atDay(5).atStartOfDay(KST).toInstant()
        val secondOrderedAt = month.plusMonths(1).atDay(1).atStartOfDay(KST).toInstant().plusSeconds(1)

        commissions.snapshotOrder(firstOrderId, buyerId, firstOrderedAt, 100_001)
        commissions.snapshotOrder(secondOrderId, buyerId, secondOrderedAt, 100_001)

        val beforeSettlement = commissions.all(0, 20).items.associateBy { it.orderId }
        assertThat(beforeSettlement[firstOrderId]?.salesAccountId).isEqualTo(salesOneId)
        assertThat(beforeSettlement[firstOrderId]?.rateBps).isEqualTo(30)
        assertThat(beforeSettlement[firstOrderId]?.commissionAmount).isEqualTo(300)
        assertThat(beforeSettlement[secondOrderId]?.salesAccountId).isEqualTo(salesTwoId)
        assertThat(beforeSettlement[secondOrderId]?.rateBps).isEqualTo(50)

        val settlement = commissions.settle(month, adminId)
        assertThat(settlement.payableCount).isEqualTo(1)
        assertThat(settlement.payableAmount).isEqualTo(300)
        assertThat(commissions.settle(month, adminId).payableCount).isZero()

        val payable = commissions.all(0, 20).items.single { it.orderId == firstOrderId }
        assertThat(payable.status).isEqualTo("PAYABLE")
        assertThat(payable.settlementMonth).isEqualTo(month.atDay(1))
        commissions.markPaid(payable.id, adminId)
        commissions.markPaid(payable.id, adminId)
        commissions.reverseOrder(firstOrderId, "PARTIAL_REFUND")

        val reversed = commissions.all(0, 20).items.single { it.orderId == firstOrderId }
        assertThat(reversed.status).isEqualTo("REVERSED")
        val eventAmounts = jdbc.queryForList(
            "SELECT amount_delta FROM sales_commission_event WHERE commission_id = (SELECT id FROM sales_commission WHERE order_id = (SELECT id FROM purchase_order WHERE public_id = ?)) ORDER BY id",
            Long::class.java,
            firstOrderId.toBytes(),
        )
        assertThat(eventAmounts).containsExactly(0L, 300L, -300L, -300L)

        val permissionCount = jdbc.queryForObject(
            """SELECT COUNT(*) FROM role_permission rp JOIN role ON role.id = rp.role_id
                JOIN permission ON permission.id = rp.permission_id
                WHERE role.code IN ('ADMIN', 'SUPER_ADMIN') AND permission.code = 'SALES_COMMISSION_SETTLE'""",
            Int::class.java,
        )
        assertThat(permissionCount).isEqualTo(2)
    }

    private fun insertAssignment(salesAccountId: Long, rate: Int, from: Instant, until: Instant?) {
        jdbc.update(
            """INSERT INTO organization_sales_assignment
                (public_id, organization_id, sales_account_id, commission_rate_bps, assignment_reason, valid_from, valid_until, assigned_by_account_id)
                VALUES (?, ?, ?, ?, 'TEST_ASSIGNMENT', ?, ?, ?)
            """.trimIndent(),
            UUID.randomUUID().toBytes(),
            organizationInternalId,
            salesAccountId,
            rate,
            Timestamp.from(from),
            until?.let(Timestamp::from),
            adminInternalId,
        )
    }

    private fun insertOrder(
        orderId: UUID,
        number: String,
        orderedAt: Instant,
        orderStatus: String,
        paidAt: Instant?,
        deliveredAt: Instant?,
    ) {
        jdbc.update(
            """INSERT INTO purchase_order
                (public_id, order_number, account_id, sales_channel_code, organization_id, status,
                 subtotal_amount, total_amount, ordered_at)
                VALUES (?, ?, ?, 'WHOLESALE', ?, ?, 100001, 100001, ?)
            """.trimIndent(),
            orderId.toBytes(),
            number,
            buyerAccountInternalId,
            organizationInternalId,
            orderStatus,
            Timestamp.from(orderedAt),
        )
        val orderInternalId = jdbc.queryForObject("SELECT id FROM purchase_order WHERE public_id = ?", Long::class.java, orderId.toBytes())!!
        val paymentStatus = if (paidAt == null) "WAITING_FOR_DEPOSIT" else "PAYMENT_CONFIRMED"
        jdbc.update(
            "INSERT INTO order_payment (order_id, payment_method, status, updated_at) VALUES (?, 'BANK_TRANSFER', ?, ?)",
            orderInternalId,
            paymentStatus,
            Timestamp.from(paidAt ?: orderedAt),
        )
        val paymentInternalId = jdbc.queryForObject("SELECT id FROM order_payment WHERE order_id = ?", Long::class.java, orderInternalId)!!
        jdbc.update(
            "INSERT INTO order_payment_status_history (payment_id, from_status, to_status, processed_by, changed_at) VALUES (?, NULL, ?, ?, ?)",
            paymentInternalId,
            paymentStatus,
            if (paidAt == null) null else adminInternalId,
            Timestamp.from(paidAt ?: orderedAt),
        )
        jdbc.update(
            """INSERT INTO order_shipment (order_id, status, processed_by, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            orderInternalId,
            if (deliveredAt == null) "READY_TO_SHIP" else "DELIVERED",
            if (deliveredAt == null) null else adminInternalId,
            Timestamp.from(orderedAt),
            Timestamp.from(deliveredAt ?: orderedAt),
        )
    }

    private fun insertAccount(loginId: String, publicId: UUID): Long {
        jdbc.update(
            "INSERT INTO account (public_id, login_id, password_hash, name, status) VALUES (?, ?, 'test-hash', ?, 'ACTIVE')",
            publicId.toBytes(),
            loginId,
            loginId,
        )
        return jdbc.queryForObject("SELECT id FROM account WHERE login_id = ?", Long::class.java, loginId)!!
    }

    private fun closedMonth(): YearMonth = YearMonth.now(KST).minusMonths(1)

    private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16).putLong(mostSignificantBits).putLong(leastSignificantBits).array()

    private companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}