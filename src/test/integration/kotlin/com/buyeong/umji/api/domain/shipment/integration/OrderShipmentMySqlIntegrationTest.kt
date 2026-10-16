package com.buyeong.umji.api.domain.shipment.integration

import com.buyeong.umji.api.domain.account.dto.OrganizationRegistrationCommandDto
import com.buyeong.umji.api.domain.account.service.OrganizationMembershipService
import com.buyeong.umji.api.domain.operation.profit.service.OperationItemProfitService
import com.buyeong.umji.api.domain.order.service.CustomerOrderListingService
import com.buyeong.umji.api.domain.order.service.OrderCancellationService
import com.buyeong.umji.api.domain.order.service.OrderService
import com.buyeong.umji.api.domain.order.service.ShippingHolidayService
import com.buyeong.umji.api.domain.payment.service.PaymentService
import com.buyeong.umji.api.domain.shipment.integration.tracking.OfficialCarrierTrackingGateway
import com.buyeong.umji.api.domain.shipment.model.CarrierTrackingStatus
import com.buyeong.umji.api.domain.shipment.model.ShipmentPackageItemRequest
import com.buyeong.umji.api.domain.shipment.model.ShipmentPackageRequest
import com.buyeong.umji.api.domain.shipment.service.BrandShippingService
import com.buyeong.umji.api.domain.shipment.service.ShipmentService
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@SpringBootTest(properties = ["spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=none"])
@ActiveProfiles("local")
@Transactional
@Rollback
class OrderShipmentMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var shipments: ShipmentService

    @Autowired
    private lateinit var orders: OrderService

    @Autowired
    private lateinit var customerOrderListing: CustomerOrderListingService

    @Autowired
    private lateinit var cancellations: OrderCancellationService

    @Autowired
    private lateinit var holidays: ShippingHolidayService

    @Autowired
    private lateinit var payments: PaymentService

    @Autowired
    private lateinit var itemProfits: OperationItemProfitService

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var groupMembership: OrganizationMembershipService

    @MockitoBean
    private lateinit var trackingSource: OfficialCarrierTrackingGateway

    @MockitoBean
    private lateinit var brandShipping: BrandShippingService

    @Test
    fun `flyway v14 creates shipment schema`() {
        val migrationCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '14' AND success = TRUE",
            Int::class.java,
        )
        val tableCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'order_shipment'",
            Long::class.java,
        )

        assertThat(migrationCount).isEqualTo(1)
        assertThat(tableCount).isEqualTo(1)
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

        val dispatch = shipments.prepareOrder(orderId)
        assertThat(dispatch.status).isEqualTo("PREPARING")
        assertThat(dispatch.changed).isTrue()
        assertReservation(reservationKey, "CONFIRMED", 9, 0)
        val preparingOrder = orders.detail(customerId, orderId)
        assertThat(preparingOrder.shippingStatus).isEqualTo("PREPARING")
        assertThat(preparingOrder.carrierCode).isNull()
        assertThat(preparingOrder.trackingNumber).isNull()

        val repeatedDispatch = shipments.prepareOrder(orderId)
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

        val delivered = shipments.markDelivered(orderId, operatorId)
        assertThat(delivered.status).isEqualTo("DELIVERED")
        assertThat(delivered.changed).isTrue()
        val repeatedDelivery = shipments.markDelivered(orderId, operatorId)
        assertThat(repeatedDelivery.changed).isFalse()
        val deliveredOrder = orders.detail(customerId, orderId)
        assertThat(deliveredOrder.shippingStatus).isEqualTo("DELIVERED")
        assertThat(deliveredOrder.trackingNumber).isEqualTo("1234567890")
    }

    @Test
    fun `customer order list refreshes only its in transit shipment before returning`() {
        val operatorId = createAccount("tracking-list-operator")
        val customerId = createAccount("tracking-list-customer")
        val otherCustomerId = createAccount("tracking-list-other-customer")
        val skuId = createSku(createProduct(createCategory()))
        val reservationKey = UUID.randomUUID()
        createStockAndReservation(skuId, reservationKey)
        val orderId = createOrder(customerId, skuId, reservationKey)
        createShipment(orderId)
        shipments.prepareOrder(orderId)
        val brandId = UUID.fromString(
            jdbc.queryForObject(
                "SELECT BIN_TO_UUID(brand.public_id) FROM brand JOIN product ON product.brand_id = brand.id JOIN product_sku sku ON sku.product_id = product.id WHERE sku.public_id = ?",
                String::class.java,
                skuId.toBytes(),
            ),
        )
        val orderItemId = UUID.fromString(
            jdbc.queryForObject(
                "SELECT BIN_TO_UUID(public_id) FROM order_item WHERE order_id = (SELECT id FROM purchase_order WHERE public_id = ?)",
                String::class.java,
                orderId.toBytes(),
            ),
        )
        org.mockito.Mockito.`when`(brandShipping.canManageBrandOrAdmin(brandId, operatorId)).thenReturn(true)
        shipments.registerPackage(
            orderId,
            brandId,
            ShipmentPackageRequest("DAESIN", "1501602023302", 0L, listOf(ShipmentPackageItemRequest(orderItemId, 1))),
            operatorId,
        )
        org.mockito.Mockito.`when`(trackingSource.lookup("DAESIN", "1501602023302"))
            .thenReturn(CarrierTrackingStatus.DELIVERED)

        assertThat(customerOrderListing.list(otherCustomerId, 0, 20).items).isEmpty()
        val page = customerOrderListing.list(customerId, 0, 20)

        assertThat(page.items).hasSize(1)
        assertThat(page.items.single().shippingStatus).isEqualTo("DELIVERED")
    }

    @Test
    fun `profit sums discounted line amount and shipment snapshots while excluding cancelled orders`() {
        val operatorId = createAccount("profit-operator")
        val customerId = createAccount("profit-customer")
        val skuId = createSku(createProduct(createCategory()))
        val reservationKey = UUID.randomUUID()
        createStockAndReservation(skuId, reservationKey)
        val orderId = createOrder(customerId, skuId, reservationKey)
        createShipment(orderId)
        shipments.prepareOrder(orderId)

        val brandId = UUID.fromString(
            jdbc.queryForObject(
                "SELECT BIN_TO_UUID(brand.public_id) FROM brand JOIN product ON product.brand_id = brand.id JOIN product_sku sku ON sku.product_id = product.id WHERE sku.public_id = ?",
                String::class.java,
                skuId.toBytes(),
            ),
        )
        val orderItemId = UUID.fromString(
            jdbc.queryForObject(
                "SELECT BIN_TO_UUID(public_id) FROM order_item WHERE order_id = (SELECT id FROM purchase_order WHERE public_id = ?)",
                String::class.java,
                orderId.toBytes(),
            ),
        )
        jdbc.update(
            "UPDATE order_item SET line_amount = 800 WHERE public_id = ?",
            orderItemId.toBytes(),
        )
        entityManager.clear()
        org.mockito.Mockito.`when`(brandShipping.canManageBrandOrAdmin(brandId, operatorId)).thenReturn(true)
        shipments.registerPackage(
            orderId,
            brandId,
            ShipmentPackageRequest("DAESIN", "PROFIT-TRACK", 200L, listOf(ShipmentPackageItemRequest(orderItemId, 1))),
            operatorId,
        )

        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        val result = itemProfits.query(today, today, skuId, brandId)
        val row = result.items.single()
        assertThat(row.salesAmount).isEqualTo(800L)
        assertThat(row.purchaseCost).isEqualTo(1000L)
        assertThat(row.shippingCost).isEqualTo(200L)
        assertThat(row.netProfit).isEqualTo(-400L)

        jdbc.update("UPDATE purchase_order SET status = 'CANCELLED' WHERE public_id = ?", orderId.toBytes())
        assertThat(itemProfits.query(today, today, null, null).items).isEmpty()
    }

    @Test
    fun `ready order cancellation releases reserved inventory and schedules manual refund`() {
        val customerId = createAccount("cancel-ready")
        val operatorId = createAccount("cancel-operator")
        val skuId = createSku(createProduct(createCategory()))
        val reservationKey = UUID.randomUUID()
        createStockAndReservation(skuId, reservationKey)
        val orderId = createOrder(customerId, skuId, reservationKey)
        createShipment(orderId)
        jdbc.update("UPDATE purchase_order SET status = 'PAID' WHERE public_id = ?", orderId.toBytes())
        jdbc.update("UPDATE order_payment SET status = 'PAYMENT_CONFIRMED' WHERE order_id = (SELECT id FROM purchase_order WHERE public_id = ?)", orderId.toBytes())

        val result = cancellations.request(customerId, orderId)

        assertThat(result.requestStatus).isEqualTo("CANCELLED")
        assertThat(orders.detail(customerId, orderId).cancellationRequestStatus).isEqualTo("CANCELLED")
        assertThat(jdbc.queryForObject("SELECT status FROM purchase_order WHERE public_id = ?", String::class.java, orderId.toBytes())).isEqualTo("CANCELLED")
        assertThat(paymentStatus(orderId)).isEqualTo("REFUND_PENDING")
        assertReservation(reservationKey, "RELEASED", 10, 0)
        val completedCancellation = jdbc.queryForMap(
            "SELECT request_status, processed_at FROM order_cancellation_history WHERE order_id = (SELECT id FROM purchase_order WHERE public_id = ?)",
            orderId.toBytes(),
        )
        assertThat(completedCancellation["request_status"]).isEqualTo("CANCELLED")
        assertThat(completedCancellation["processed_at"]).isNotNull()
        assertThat(payments.updateStatus(orderId, "REFUNDED", operatorId, "환불 완료").paymentStatus).isEqualTo("REFUNDED")
        assertThat(paymentStatus(orderId)).isEqualTo("REFUNDED")
    }

    @Test
    fun `preparing order cancellation needs operator approval and restores confirmed inventory`() {
        val customerId = createAccount("cancel-preparing")
        val operatorId = createAccount("cancel-reviewer")
        val skuId = createSku(createProduct(createCategory()))
        val reservationKey = UUID.randomUUID()
        createStockAndReservation(skuId, reservationKey)
        val orderId = createOrder(customerId, skuId, reservationKey)
        createShipment(orderId)
        shipments.prepareOrder(orderId)

        assertThat(cancellations.request(customerId, orderId).requestStatus).isEqualTo("PENDING")
        assertThat(orders.detail(customerId, orderId).cancellationRequestStatus).isEqualTo("PENDING")
        val pendingCancellation = jdbc.queryForMap(
            "SELECT request_status, processed_at FROM order_cancellation_history WHERE order_id = (SELECT id FROM purchase_order WHERE public_id = ?)",
            orderId.toBytes(),
        )
        assertThat(pendingCancellation["request_status"]).isEqualTo("PENDING")
        assertThat(pendingCancellation["processed_at"]).isNull()
        assertThat(cancellations.queue(0, 20).items.map { it.orderId }).contains(orderId)
        assertThat(cancellations.resolve(orderId, true, operatorId).orderStatus).isEqualTo("CANCELLED")
        assertThat(
            jdbc.queryForObject(
                "SELECT request_status FROM order_cancellation_history WHERE order_id = (SELECT id FROM purchase_order WHERE public_id = ?)",
                String::class.java,
                orderId.toBytes(),
            ),
        ).isEqualTo("APPROVED")
        assertThat(
            jdbc.queryForObject(
                "SELECT processed_at FROM order_cancellation_history WHERE order_id = (SELECT id FROM purchase_order WHERE public_id = ?)",
                Timestamp::class.java,
                orderId.toBytes(),
            ),
        ).isNotNull()
        assertReservation(reservationKey, "RESTORED", 10, 0)
    }

    @Test
    fun `registered holiday is excluded from shipping preparation calendar`() {
        val operatorId = createAccount("holiday-operator")
        val date = java.time.LocalDate.of(2026, 12, 25)
        holidays.register(date, "Holiday test", operatorId)
        assertThat(holidays.isHoliday(date)).isTrue()
        holidays.remove(date)
        assertThat(holidays.isHoliday(date)).isFalse()
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
        groupMembership.register(publicId, OrganizationRegistrationCommandDto("INDIVIDUAL", null))
        return publicId
    }

    private fun createCategory(): Long {
        val publicId = UUID.randomUUID()
        val channelId = jdbc.queryForObject("SELECT id FROM sales_channel WHERE code = 'WHOLESALE'", Long::class.java)!!
        jdbc.update(
            "INSERT INTO category (public_id, sales_channel_id, name, path, depth, display_status) VALUES (?, ?, 'Shipment test', ?, 0, 'VISIBLE')",
            publicId.toBytes(),
            channelId,
            "shipment-${UUID.randomUUID()}",
        )
        return jdbc.queryForObject("SELECT id FROM category WHERE public_id = ?", Long::class.java, publicId.toBytes())!!
    }

    private fun createProduct(categoryId: Long): Long {
        val publicId = UUID.randomUUID()
        val brandId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO brand (public_id, name, display_status) VALUES (?, ?, 'VISIBLE')",
            brandId.toBytes(),
            "Shipment brand ${UUID.randomUUID()}",
        )
        val brandInternalId = jdbc.queryForObject("SELECT id FROM brand WHERE public_id = ?", Long::class.java, brandId.toBytes())!!
        jdbc.update(
            "INSERT INTO product (public_id, brand_id, category_id, name, display_status, sales_status) VALUES (?, ?, ?, 'Shipment test', 'VISIBLE', 'ON_SALE')",
            publicId.toBytes(),
            brandInternalId,
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
        val skuInternalId = jdbc.queryForObject("SELECT id FROM product_sku WHERE public_id = ?", Long::class.java, publicId.toBytes())!!
        val channelId = jdbc.queryForObject("SELECT id FROM sales_channel WHERE code = 'WHOLESALE'", Long::class.java)!!
        jdbc.update(
            "INSERT INTO sales_offer (public_id, sales_channel_id, product_sku_id, sale_price, sales_status) VALUES (?, ?, ?, 1000, 'ON_SALE')",
            UUID.randomUUID().toBytes(),
            channelId,
            skuInternalId,
        )
        return publicId
    }

    private fun createStockAndReservation(skuId: UUID, reservationKey: UUID) {
        val skuInternalId = jdbc.queryForObject("SELECT id FROM product_sku WHERE public_id = ?", Long::class.java, skuId.toBytes())!!
        jdbc.update("INSERT INTO inventory_stock (sku_id, on_hand_quantity, reserved_quantity, safety_stock_quantity) VALUES (?, 10, 1, 0)", skuInternalId)
        jdbc.update(
            "INSERT INTO inventory_cost_lot (sku_id, container_reference, received_quantity, remaining_quantity, reserved_quantity, total_purchase_cost, remaining_cost_amount) VALUES (?, ?, 10, 10, 1, 10000, 10000)",
            skuInternalId,
            "SHIPMENT-${UUID.randomUUID()}",
        )
        jdbc.update(
            "INSERT INTO stock_reservation (reservation_key, sku_id, quantity, status) VALUES (?, ?, 1, 'RESERVED')",
            reservationKey.toBytes(),
            skuInternalId,
        )
        val reservationInternalId = jdbc.queryForObject(
            "SELECT id FROM stock_reservation WHERE reservation_key = ?",
            Long::class.java,
            reservationKey.toBytes(),
        )!!
        val lotInternalId = jdbc.queryForObject(
            "SELECT id FROM inventory_cost_lot WHERE sku_id = ? AND container_reference LIKE 'SHIPMENT-%' ORDER BY id DESC LIMIT 1",
            Long::class.java,
            skuInternalId,
        )!!
        jdbc.update(
            "INSERT INTO stock_reservation_lot_allocation (reservation_id, lot_id, quantity, cost_amount, status) VALUES (?, ?, 1, NULL, 'RESERVED')",
            reservationInternalId,
            lotInternalId,
        )
    }

    private fun createOrder(customerId: UUID, skuId: UUID, reservationKey: UUID): UUID {
        val accountInternalId = jdbc.queryForObject("SELECT id FROM account WHERE public_id = ?", Long::class.java, customerId.toBytes())!!
        val organizationInternalId = jdbc.queryForObject(
            "SELECT organization_id FROM organization_member WHERE account_id = ? AND status = 'ACTIVE'",
            Long::class.java,
            accountInternalId
        )!!
        val skuInternalId = jdbc.queryForObject("SELECT id FROM product_sku WHERE public_id = ?", Long::class.java, skuId.toBytes())!!
        val offerInternalId = jdbc.queryForObject(
            "SELECT id FROM sales_offer WHERE product_sku_id = ?",
            Long::class.java,
            skuInternalId,
        )!!
        val orderId = UUID.randomUUID()
        val now = Instant.now()
        jdbc.update(
            """INSERT INTO purchase_order
                (public_id, order_number, account_id, created_by_account_id, organization_id, status, subtotal_amount, total_amount, ordered_at)
                VALUES (?, ?, ?, ?, ?, 'PENDING_PAYMENT', 1000, 1000, ?)
            """.trimIndent(),
            orderId.toBytes(),
            "SHIP-${UUID.randomUUID().toString().take(8)}",
            accountInternalId,
            accountInternalId,
            organizationInternalId,
            Timestamp.from(now),
        )
        val orderInternalId = jdbc.queryForObject("SELECT id FROM purchase_order WHERE public_id = ?", Long::class.java, orderId.toBytes())!!
        jdbc.update(
            """INSERT INTO order_item
                (public_id, order_id, sku_id, sales_offer_id, product_name, sku_name, sku_code, unit_price, quantity, line_amount, reservation_key, status)
                VALUES (?, ?, ?, ?, 'Shipment test', 'Shipment test SKU', ?, 1000, 1, 1000, ?, 'RESERVED')
            """.trimIndent(),
            UUID.randomUUID().toBytes(),
            orderInternalId,
            skuInternalId,
            offerInternalId,
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