package com.buyeong.umji.api.shipment.adapter

import com.buyeong.umji.api.operation.shipment.adapter.`in`.web.TransactionalShipmentUseCase
import com.buyeong.umji.api.order.application.port.`in`.OrderUseCase
import org.assertj.core.api.Assertions.assertThat
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
import java.util.UUID

@SpringBootTest(properties = ["spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=none"])
@ActiveProfiles("local")
@Transactional
@Rollback
class OrderShipmentMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var shipments: TransactionalShipmentUseCase

    @Autowired
    private lateinit var orders: OrderUseCase

    @Test
    fun `flyway v14 creates shipment rows for existing orders`() {
        val migrationCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '14' AND success = TRUE",
            Int::class.java,
        )
        val orderCount = jdbc.queryForObject("SELECT COUNT(*) FROM purchase_order", Long::class.java)
        val shipmentCount = jdbc.queryForObject("SELECT COUNT(*) FROM order_shipment", Long::class.java)
        val missingShipmentCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM purchase_order o LEFT JOIN order_shipment s ON s.order_id = o.id WHERE s.id IS NULL",
            Long::class.java,
        )

        assertThat(migrationCount).isEqualTo(1)
        assertThat(shipmentCount).isEqualTo(orderCount)
        assertThat(missingShipmentCount).isZero()
    }

    @Test
    fun `tracking details persist and appear in the customer's order`() {
        val operatorId = createAccount("shipment-operator")
        val customerId = createAccount("shipment-customer")
        val categoryId = createCategory()
        val productId = createProduct(categoryId)
        val skuId = createSku(productId)
        val reservationKey = UUID.randomUUID()
        createStockAndReservation(skuId, reservationKey)
        val orderId = createOrder(customerId, skuId, reservationKey)
        createShipment(orderId)

        assertThat(paymentStatus(orderId)).isEqualTo("WAITING_FOR_DEPOSIT")
        val readyOrder = orders.detail(customerId, orderId)
        assertThat(readyOrder.shippingStatus).isEqualTo("READY_TO_SHIP")
        assertThat(readyOrder.carrierCode).isNull()
        assertThat(readyOrder.trackingNumber).isNull()

        val dispatch = shipments.beginDispatch(orderId, operatorId)
        assertThat(dispatch.status).isEqualTo("PREPARING")
        assertThat(dispatch.changed).isTrue()
        assertReservation(reservationKey, "RESERVED", 10, 1)
        val preparingOrder = orders.detail(customerId, orderId)
        assertThat(preparingOrder.shippingStatus).isEqualTo("PREPARING")
        assertThat(preparingOrder.carrierCode).isNull()
        assertThat(preparingOrder.trackingNumber).isNull()

        val repeatedDispatch = shipments.beginDispatch(orderId, operatorId)
        assertThat(repeatedDispatch.status).isEqualTo("PREPARING")
        assertThat(repeatedDispatch.changed).isFalse()

        val tracking = shipments.registerTracking(orderId, " CJ ", " 1234567890 ", operatorId)
        assertThat(tracking.status).isEqualTo("IN_TRANSIT")
        assertThat(tracking.changed).isTrue()
        assertThat(tracking.carrierCode).isEqualTo("CJ")
        assertThat(tracking.trackingNumber).isEqualTo("1234567890")
        assertReservation(reservationKey, "CONFIRMED", 9, 0)

        val repeatedTracking = shipments.registerTracking(orderId, "CJ", "1234567890", operatorId)
        assertThat(repeatedTracking.changed).isFalse()
        assertReservation(reservationKey, "CONFIRMED", 9, 0)

        val persisted = jdbc.queryForMap(
            """SELECT s.status, s.carrier_code, s.tracking_number, a.public_id AS processed_by
                FROM order_shipment s
                LEFT JOIN account a ON a.id = s.processed_by
                WHERE s.order_id = (SELECT id FROM purchase_order WHERE public_id = ?)
            """.trimIndent(),
            orderId.toBytes(),
        )
        assertThat(persisted["status"]).isEqualTo("IN_TRANSIT")
        assertThat(persisted["carrier_code"]).isEqualTo("CJ")
        assertThat(persisted["tracking_number"]).isEqualTo("1234567890")
        assertThat(persisted["processed_by"]).isEqualTo(operatorId.toBytes())

        val order = orders.detail(customerId, orderId)
        assertThat(order.shippingStatus).isEqualTo("IN_TRANSIT")
        assertThat(order.carrierCode).isEqualTo("CJ")
        assertThat(order.trackingNumber).isEqualTo("1234567890")
    }

    private fun createAccount(suffix: String): UUID {
        val publicId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO account (public_id, login_id, password_hash, name, phone, status) VALUES (?, ?, 'test-hash', ?, ?, 'ACTIVE')",
            publicId.toBytes(),
            "shipment-$suffix-${UUID.randomUUID()}",
            suffix,
            "555${UUID.randomUUID().toString().take(7)}",
        )
        return publicId
    }

    private fun createCategory(): Long {
        val publicId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO category (public_id, name, path, depth, display_status) VALUES (?, 'Shipment test', ?, 0, 'VISIBLE')",
            publicId.toBytes(),
            "shipment-${UUID.randomUUID()}",
        )
        return jdbc.queryForObject("SELECT id FROM category WHERE public_id = ?", Long::class.java, publicId.toBytes())!!
    }

    private fun createProduct(categoryId: Long): Long {
        val publicId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO product (public_id, category_id, name, display_status, sales_status) VALUES (?, ?, 'Shipment test', 'VISIBLE', 'ON_SALE')",
            publicId.toBytes(),
            categoryId,
        )
        return jdbc.queryForObject("SELECT id FROM product WHERE public_id = ?", Long::class.java, publicId.toBytes())!!
    }

    private fun createSku(productId: Long): UUID {
        val publicId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO product_sku (public_id, product_id, sku_code, name, sale_price, sales_status) VALUES (?, ?, ?, 'Shipment test SKU', 1000, 'ON_SALE')",
            publicId.toBytes(),
            productId,
            "SHIP-${UUID.randomUUID().toString().take(8)}",
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

    private fun createOrder(customerId: UUID, skuId: UUID, reservationKey: UUID): UUID {
        val accountInternalId = jdbc.queryForObject("SELECT id FROM account WHERE public_id = ?", Long::class.java, customerId.toBytes())!!
        val skuInternalId = jdbc.queryForObject("SELECT id FROM product_sku WHERE public_id = ?", Long::class.java, skuId.toBytes())!!
        val orderId = UUID.randomUUID()
        val now = Instant.now()
        jdbc.update(
            """INSERT INTO purchase_order
                (public_id, order_number, account_id, status, subtotal_amount, total_amount, ordered_at)
                VALUES (?, ?, ?, 'PENDING_PAYMENT', 1000, 1000, ?)
            """.trimIndent(),
            orderId.toBytes(),
            "SHIP-${UUID.randomUUID().toString().take(8)}",
            accountInternalId,
            Timestamp.from(now),
        )
        val orderInternalId = jdbc.queryForObject("SELECT id FROM purchase_order WHERE public_id = ?", Long::class.java, orderId.toBytes())!!
        jdbc.update(
            """INSERT INTO order_item
                (public_id, order_id, sku_id, product_name, sku_name, sku_code, unit_price, quantity, line_amount, reservation_key, status)
                VALUES (?, ?, ?, 'Shipment test', 'Shipment test SKU', ?, 1000, 1, 1000, ?, 'RESERVED')
            """.trimIndent(),
            UUID.randomUUID().toBytes(),
            orderInternalId,
            skuInternalId,
            "SHIP-${UUID.randomUUID()}",
            reservationKey.toBytes(),
        )
        jdbc.update(
            "INSERT INTO order_payment (order_id, payment_method, status, updated_at) VALUES (?, 'BANK_TRANSFER', 'WAITING_FOR_DEPOSIT', ?)",
            orderInternalId,
            Timestamp.from(now),
        )
        return orderId
    }

    private fun createShipment(orderId: UUID) {
        val orderInternalId = jdbc.queryForObject("SELECT id FROM purchase_order WHERE public_id = ?", Long::class.java, orderId.toBytes())!!
        val orderedAt = jdbc.queryForObject("SELECT ordered_at FROM purchase_order WHERE id = ?", Timestamp::class.java, orderInternalId)!!.toInstant()
        jdbc.update(
            "INSERT INTO order_shipment (order_id, status, created_at, updated_at) VALUES (?, 'READY_TO_SHIP', ?, ?)",
            orderInternalId,
            Timestamp.from(orderedAt),
            Timestamp.from(orderedAt),
        )
    }

    private fun paymentStatus(orderId: UUID): String = jdbc.queryForObject(
        "SELECT payment.status FROM order_payment payment JOIN purchase_order purchase_order ON purchase_order.id = payment.order_id WHERE purchase_order.public_id = ?",
        String::class.java,
        orderId.toBytes(),
    )!!

    private fun assertReservation(reservationKey: UUID, status: String, onHand: Int, reserved: Int) {
        assertThat(
            jdbc.queryForObject(
                "SELECT status FROM stock_reservation WHERE reservation_key = ?",
                String::class.java,
                reservationKey.toBytes(),
            ),
        ).isEqualTo(status)
        val stock = jdbc.queryForMap(
            "SELECT on_hand_quantity, reserved_quantity FROM inventory_stock WHERE sku_id = (SELECT sku_id FROM stock_reservation WHERE reservation_key = ?)",
            reservationKey.toBytes(),
        )
        assertThat(stock["on_hand_quantity"]).isEqualTo(onHand)
        assertThat(stock["reserved_quantity"]).isEqualTo(reserved)
    }

    private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16).putLong(mostSignificantBits).putLong(leastSignificantBits).array()
}
