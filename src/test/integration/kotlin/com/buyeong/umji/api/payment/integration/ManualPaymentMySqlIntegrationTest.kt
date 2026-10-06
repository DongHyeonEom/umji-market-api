package com.buyeong.umji.api.payment.integration

import com.buyeong.umji.api.account.model.BuyerGroupRegistrationCommand
import com.buyeong.umji.api.account.service.BuyerGroupMembershipService
import com.buyeong.umji.api.order.service.OrderService
import com.buyeong.umji.api.payment.service.PaymentService
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(properties = ["spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=none"])
@ActiveProfiles("local")
@Transactional
@Rollback
class ManualPaymentMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var transactionalPayments: PaymentService

    @Autowired
    private lateinit var orders: OrderService

    @Autowired
    private lateinit var groupMembership: BuyerGroupMembershipService

    @Test
    fun `general payment issue is visible and can be resolved with operator history`() {
        val operatorId = createAccount("payment-issue-resolution-operator")
        val customerId = createAccount("payment-issue-resolution-customer")
        val categoryId = createCategory()
        val productId = createProduct(categoryId)
        val skuId = createSku(productId)
        val reservationKey = UUID.randomUUID()
        createStockAndReservation(skuId, reservationKey)
        val orderId = createOrder(customerId, skuId, reservationKey)
        createPayment(orderId)
        createShipment(orderId)

        val issue = transactionalPayments.updateStatus(orderId, "PAYMENT_ISSUE_REVIEW_REQUIRED", operatorId)
        assertThat(issue.paymentStatus).isEqualTo("PAYMENT_ISSUE_REVIEW_REQUIRED")
        assertThat(issue.orderStatus).isEqualTo("PENDING_PAYMENT")

        val repeatedIssue = transactionalPayments.updateStatus(orderId, "PAYMENT_ISSUE_REVIEW_REQUIRED", operatorId)
        assertThat(repeatedIssue.changed).isFalse()

        val issueQueue = transactionalPayments.queue("PAYMENT_ISSUE_REVIEW_REQUIRED", 0, 10)
        assertThat(issueQueue.items.map { it.orderId }).contains(orderId)

        val customerOrder = orders.detail(customerId, orderId)
        assertThat(customerOrder.paymentStatus).isEqualTo("PAYMENT_ISSUE_REVIEW_REQUIRED")
        assertThat(customerOrder.status).isEqualTo("PENDING_PAYMENT")
        assertThat(customerOrder.shippingStatus).isEqualTo("READY_TO_SHIP")

        assertThat(transactionalPayments.updateStatus(orderId, "PARTIAL_PAYMENT_REVIEW_REQUIRED", operatorId).paymentStatus)
            .isEqualTo("PARTIAL_PAYMENT_REVIEW_REQUIRED")
        assertThat(transactionalPayments.updateStatus(orderId, "PAYMENT_ISSUE_REVIEW_REQUIRED", operatorId).paymentStatus)
            .isEqualTo("PAYMENT_ISSUE_REVIEW_REQUIRED")
        assertThat(transactionalPayments.updateStatus(orderId, "WAITING_FOR_DEPOSIT", operatorId).paymentStatus)
            .isEqualTo("WAITING_FOR_DEPOSIT")
        assertThat(transactionalPayments.updateStatus(orderId, "PAYMENT_ISSUE_REVIEW_REQUIRED", operatorId).paymentStatus)
            .isEqualTo("PAYMENT_ISSUE_REVIEW_REQUIRED")
        val resolved = transactionalPayments.updateStatus(orderId, "PAYMENT_CONFIRMED", operatorId)
        assertThat(resolved.paymentStatus).isEqualTo("PAYMENT_CONFIRMED")
        assertThat(resolved.orderStatus).isEqualTo("PAID")

        val actorInternalId = jdbc.queryForObject(
            "SELECT id FROM account WHERE public_id = ?",
            Long::class.java,
            operatorId.toBytes(),
        )
        val history = jdbc.queryForList(
            """SELECT history.from_status, history.to_status, history.processed_by
                FROM order_payment_status_history history
                JOIN order_payment payment ON payment.id = history.payment_id
                JOIN purchase_order purchase_order ON purchase_order.id = payment.order_id
                WHERE purchase_order.public_id = ? ORDER BY history.id""",
            orderId.toBytes(),
        )
        val transitions = listOf(
            null to "WAITING_FOR_DEPOSIT",
            "WAITING_FOR_DEPOSIT" to "PAYMENT_ISSUE_REVIEW_REQUIRED",
            "PAYMENT_ISSUE_REVIEW_REQUIRED" to "PARTIAL_PAYMENT_REVIEW_REQUIRED",
            "PARTIAL_PAYMENT_REVIEW_REQUIRED" to "PAYMENT_ISSUE_REVIEW_REQUIRED",
            "PAYMENT_ISSUE_REVIEW_REQUIRED" to "WAITING_FOR_DEPOSIT",
            "WAITING_FOR_DEPOSIT" to "PAYMENT_ISSUE_REVIEW_REQUIRED",
            "PAYMENT_ISSUE_REVIEW_REQUIRED" to "PAYMENT_CONFIRMED",
        )
        assertThat(history).hasSize(transitions.size)
        history.forEachIndexed { index, row ->
            assertThat(row["from_status"]).isEqualTo(transitions[index].first)
            assertThat(row["to_status"]).isEqualTo(transitions[index].second)
            if (index > 0) assertThat(row["processed_by"]).isEqualTo(actorInternalId)
        }
        assertThat(jdbc.queryForObject("SELECT status FROM stock_reservation WHERE reservation_key = ?", String::class.java, reservationKey.toBytes()))
            .isEqualTo("RESERVED")
        assertShipmentStatus(orderId, "READY_TO_SHIP")
    }

    @Test
    fun `flyway v15 allows payment issue status and operator change is persisted in history`() {
        val migrationCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '15' AND success = TRUE",
            Int::class.java,
        )
        assertThat(migrationCount).isEqualTo(1)

        val actor = createAccount("payment-issue-operator")
        val customer = createAccount("payment-issue-customer")
        val categoryId = createCategory()
        val productId = createProduct(categoryId)
        val skuId = createSku(productId)
        val reservationKey = UUID.randomUUID()
        createStockAndReservation(skuId, reservationKey)
        val orderId = createOrder(customer, skuId, reservationKey)
        createPayment(orderId)

        val result = transactionalPayments.updateStatus(orderId, "PAYMENT_ISSUE_REVIEW_REQUIRED", actor)

        assertThat(result.orderStatus).isEqualTo("PENDING_PAYMENT")
        assertThat(result.paymentStatus).isEqualTo("PAYMENT_ISSUE_REVIEW_REQUIRED")
        val history = jdbc.queryForMap(
            """SELECT history.from_status, history.to_status, history.processed_by
                FROM order_payment_status_history history
                JOIN order_payment payment ON payment.id = history.payment_id
                JOIN purchase_order purchase_order ON purchase_order.id = payment.order_id
                WHERE purchase_order.public_id = ? AND history.to_status = 'PAYMENT_ISSUE_REVIEW_REQUIRED'""",
            orderId.toBytes(),
        )
        assertThat(history["from_status"]).isEqualTo("WAITING_FOR_DEPOSIT")
        assertThat(history["to_status"]).isEqualTo("PAYMENT_ISSUE_REVIEW_REQUIRED")
        assertThat(history["processed_by"]).isEqualTo(
            jdbc.queryForObject("SELECT id FROM account WHERE public_id = ?", Long::class.java, actor.toBytes()),
        )

        assertThatThrownBy {
            jdbc.update(
                "UPDATE order_payment SET status = 'UNSUPPORTED_PAYMENT_STATUS' WHERE order_id = (SELECT id FROM purchase_order WHERE public_id = ?)",
                orderId.toBytes(),
            )
        }.isInstanceOf(DataAccessException::class.java)
    }

    @Test
    fun `flyway initializes payment states and mysql supports the operator payment queue`() {
        val migrationCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '12' AND success = TRUE",
            Int::class.java,
        )
        assertThat(migrationCount).isEqualTo(1)

        val expectedQueueSize = jdbc.queryForObject(
            "SELECT COUNT(*) FROM order_payment WHERE status IN ('WAITING_FOR_DEPOSIT', 'PARTIAL_PAYMENT_REVIEW_REQUIRED')",
            Long::class.java,
        )
        val result = transactionalPayments.queue(null, 0, 10)

        assertThat(result.totalElements).isEqualTo(expectedQueueSize)
        assertThat(result.items).allSatisfy { item ->
            assertThat(item.paymentMethod).isEqualTo("BANK_TRANSFER")
            assertThat(item.paymentStatus).isIn("WAITING_FOR_DEPOSIT", "PARTIAL_PAYMENT_REVIEW_REQUIRED")
        }
    }

    @Test
    fun `partial and full confirmation update payment while inventory reservation stays for shipment`() {
        val actor = createAccount("payment-operator")
        val customer = createAccount("payment-customer")
        val categoryId = createCategory()
        val productId = createProduct(categoryId)
        val skuId = createSku(productId)
        val reservationKey = UUID.randomUUID()
        createStockAndReservation(skuId, reservationKey)
        val orderId = createOrder(customer, skuId, reservationKey)
        createPayment(orderId)
        createShipment(orderId)

        val partial = transactionalPayments.updateStatus(orderId, "PARTIAL_PAYMENT_REVIEW_REQUIRED", actor)

        assertThat(partial.orderStatus).isEqualTo("PENDING_PAYMENT")
        assertThat(partial.paymentStatus).isEqualTo("PARTIAL_PAYMENT_REVIEW_REQUIRED")
        assertThat(
            jdbc.queryForObject("SELECT reserved_quantity FROM inventory_stock WHERE sku_id = (SELECT id FROM product_sku WHERE public_id = ?)", Int::class.java, skuId.toBytes()),
        ).isEqualTo(1)
        assertShipmentStatus(orderId, "READY_TO_SHIP")

        val confirmed = transactionalPayments.updateStatus(orderId, "PAYMENT_CONFIRMED", actor)

        assertThat(confirmed.orderStatus).isEqualTo("PAID")
        assertThat(confirmed.paymentStatus).isEqualTo("PAYMENT_CONFIRMED")
        assertThat(jdbc.queryForObject("SELECT status FROM purchase_order WHERE public_id = ?", String::class.java, orderId.toBytes())).isEqualTo("PAID")
        assertThat(jdbc.queryForObject("SELECT status FROM stock_reservation WHERE reservation_key = ?", String::class.java, reservationKey.toBytes())).isEqualTo("RESERVED")
        assertThat(
            jdbc.queryForObject("SELECT reserved_quantity FROM inventory_stock WHERE sku_id = (SELECT id FROM product_sku WHERE public_id = ?)", Int::class.java, skuId.toBytes()),
        ).isEqualTo(1)
        assertThat(
            jdbc.queryForObject("SELECT on_hand_quantity FROM inventory_stock WHERE sku_id = (SELECT id FROM product_sku WHERE public_id = ?)", Int::class.java, skuId.toBytes()),
        ).isEqualTo(10)
        assertThat(
            jdbc.queryForObject(
                "SELECT on_hand_quantity - reserved_quantity FROM inventory_stock WHERE sku_id = (SELECT id FROM product_sku WHERE public_id = ?)",
                Int::class.java,
                skuId.toBytes(),
            ),
        ).isEqualTo(9)
        assertShipmentStatus(orderId, "READY_TO_SHIP")
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM order_payment_status_history WHERE payment_id = (SELECT id FROM order_payment WHERE order_id = (SELECT id FROM purchase_order WHERE public_id = ?))",
                Int::class.java,
                orderId.toBytes(),
            ),
        ).isEqualTo(3)
    }

    private fun createAccount(suffix: String): UUID {
        val publicId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO account (public_id, login_id, password_hash, name, phone, status) VALUES (?, ?, 'test-hash', ?, '5550100', 'ACTIVE')",
            publicId.toBytes(),
            "payment-$suffix-${UUID.randomUUID()}",
            suffix,
        )
        groupMembership.register(publicId, BuyerGroupRegistrationCommand("INDIVIDUAL", null))
        return publicId
    }

    private fun createCategory(): Long {
        val publicId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO category (public_id, name, path, depth, display_status) VALUES (?, 'Payment test', 'payment-test', 0, 'VISIBLE')",
            publicId.toBytes(),
        )
        return jdbc.queryForObject("SELECT id FROM category WHERE public_id = ?", Long::class.java, publicId.toBytes())!!
    }

    private fun createProduct(categoryId: Long): Long {
        val publicId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO product (public_id, category_id, name, display_status, sales_status) VALUES (?, ?, 'Payment test', 'VISIBLE', 'ON_SALE')",
            publicId.toBytes(),
            categoryId,
        )
        return jdbc.queryForObject("SELECT id FROM product WHERE public_id = ?", Long::class.java, publicId.toBytes())!!
    }

    private fun createSku(productId: Long): UUID {
        val publicId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO product_sku (public_id, product_id, sku_code, name, sale_price, sales_status) VALUES (?, ?, ?, 'Payment test SKU', 1000, 'ON_SALE')",
            publicId.toBytes(),
            productId,
            "PAY-${UUID.randomUUID()}",
        )
        return publicId
    }

    private fun createStockAndReservation(skuId: UUID, reservationKey: UUID) {
        val skuInternalId = jdbc.queryForObject("SELECT id FROM product_sku WHERE public_id = ?", Long::class.java, skuId.toBytes())!!
        jdbc.update("INSERT INTO inventory_stock (sku_id, on_hand_quantity, reserved_quantity, safety_stock_quantity) VALUES (?, 10, 1, 0)", skuInternalId)
        jdbc.update(
            "INSERT INTO stock_reservation (reservation_key, sku_id, quantity, status) VALUES (?, ?, 1, 'RESERVED')",
            reservationKey.toBytes(),
            skuInternalId,
        )
    }

    private fun createOrder(accountId: UUID, skuId: UUID, reservationKey: UUID): UUID {
        val accountInternalId = jdbc.queryForObject("SELECT id FROM account WHERE public_id = ?", Long::class.java, accountId.toBytes())!!
        val buyerGroupInternalId = jdbc.queryForObject("SELECT buyer_group_id FROM buyer_group_member WHERE account_id = ? AND status = 'ACTIVE'", Long::class.java, accountInternalId)!!
        val skuInternalId = jdbc.queryForObject("SELECT id FROM product_sku WHERE public_id = ?", Long::class.java, skuId.toBytes())!!
        val orderId = UUID.randomUUID()
        val now = Instant.now()
        jdbc.update(
            "INSERT INTO purchase_order (public_id, order_number, account_id, buyer_group_id, status, subtotal_amount, total_amount, ordered_at) VALUES (?, ?, ?, ?, 'PENDING_PAYMENT', 1000, 1000, ?)",
            orderId.toBytes(),
            "PAY-${UUID.randomUUID()}",
            accountInternalId,
            buyerGroupInternalId,
            Timestamp.from(now),
        )
        val orderInternalId = jdbc.queryForObject("SELECT id FROM purchase_order WHERE public_id = ?", Long::class.java, orderId.toBytes())!!
        jdbc.update(
            "INSERT INTO order_item (public_id, order_id, sku_id, product_name, sku_name, sku_code, unit_price, quantity, line_amount, reservation_key, status) VALUES (?, ?, ?, 'Payment test', 'Payment test SKU', ?, 1000, 1, 1000, ?, 'RESERVED')",
            UUID.randomUUID().toBytes(),
            orderInternalId,
            skuInternalId,
            "PAY-${UUID.randomUUID()}",
            reservationKey.toBytes(),
        )
        return orderId
    }

    private fun createPayment(orderId: UUID) {
        val orderInternalId = jdbc.queryForObject("SELECT id FROM purchase_order WHERE public_id = ?", Long::class.java, orderId.toBytes())!!
        jdbc.update(
            "INSERT INTO order_payment (order_id, payment_method, status, updated_at) VALUES (?, 'BANK_TRANSFER', 'WAITING_FOR_DEPOSIT', ?)",
            orderInternalId,
            Timestamp.from(Instant.now()),
        )
        val paymentId = jdbc.queryForObject("SELECT id FROM order_payment WHERE order_id = ?", Long::class.java, orderInternalId)!!
        jdbc.update(
            "INSERT INTO order_payment_status_history (payment_id, to_status, changed_at) VALUES (?, 'WAITING_FOR_DEPOSIT', ?)",
            paymentId,
            Timestamp.from(Instant.now()),
        )
    }

    private fun createShipment(orderId: UUID) {
        val orderInternalId = jdbc.queryForObject(
            "SELECT id FROM purchase_order WHERE public_id = ?",
            Long::class.java,
            orderId.toBytes(),
        )!!
        val orderedAt = jdbc.queryForObject(
            "SELECT ordered_at FROM purchase_order WHERE id = ?",
            Timestamp::class.java,
            orderInternalId,
        )!!.toInstant()
        jdbc.update(
            "INSERT INTO order_shipment (order_id, status, created_at, updated_at) VALUES (?, 'READY_TO_SHIP', ?, ?)",
            orderInternalId,
            Timestamp.from(orderedAt),
            Timestamp.from(orderedAt),
        )
    }

    private fun assertShipmentStatus(orderId: UUID, expectedStatus: String) {
        val status = jdbc.queryForObject(
            "SELECT status FROM order_shipment WHERE order_id = (SELECT id FROM purchase_order WHERE public_id = ?)",
            String::class.java,
            orderId.toBytes(),
        )
        assertThat(status).isEqualTo(expectedStatus)
    }

    private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16).putLong(mostSignificantBits).putLong(leastSignificantBits).array()
}
