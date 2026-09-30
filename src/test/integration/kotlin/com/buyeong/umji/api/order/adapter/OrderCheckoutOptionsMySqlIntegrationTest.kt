package com.buyeong.umji.api.order.adapter

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
import java.util.UUID

@SpringBootTest(
    properties = [
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "umji.payment.bank-account.standard.bank-name=Standard Bank",
        "umji.payment.bank-account.standard.account-number=111-222",
        "umji.payment.bank-account.standard.account-holder=Standard Holder",
        "umji.payment.bank-account.tax-invoice.bank-name=Tax Bank",
        "umji.payment.bank-account.tax-invoice.account-number=333-444",
        "umji.payment.bank-account.tax-invoice.account-holder=Tax Holder",
    ],
)
@ActiveProfiles("local")
@Transactional
@Rollback
class OrderCheckoutOptionsMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var orders: OrderUseCase

    @Test
    fun `flyway v13 persists invoice preference and order-specific bank account snapshots`() {
        val migrationCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '13' AND success = TRUE",
            Int::class.java,
        )
        assertThat(migrationCount).isEqualTo(1)

        val accountId = createAccount()
        val categoryId = createCategory()
        val productId = createProduct(categoryId)
        val skuId = createSku(productId)
        createStock(skuId)
        createCartWithItem(accountId, skuId)

        val initialOptions = orders.checkoutOptions(accountId)
        assertThat(initialOptions.defaultTaxInvoiceRequested).isFalse()
        assertThat(initialOptions.standardBankAccount.bankName).isEqualTo("Standard Bank")
        assertThat(initialOptions.taxInvoiceBankAccount.bankName).isEqualTo("Tax Bank")
        assertThat(
            jdbc.queryForObject(
                "SELECT default_tax_invoice_requested FROM account WHERE public_id = ?",
                Boolean::class.java,
                accountId.toBytes(),
            ),
        ).isFalse()

        val invoiceOrder = orders.create(accountId, true, true)
        assertThat(invoiceOrder.taxInvoiceRequested).isTrue()
        assertThat(invoiceOrder.depositBankName).isEqualTo("Tax Bank")
        assertThat(invoiceOrder.depositAccountNumber).isEqualTo("333-444")
        assertThat(invoiceOrder.depositAccountHolder).isEqualTo("Tax Holder")
        assertPersistedOrderSnapshot(invoiceOrder.id, true, "Tax Bank", "333-444", "Tax Holder")
        assertThat(orders.checkoutOptions(accountId).defaultTaxInvoiceRequested).isTrue()

        val standardAccountId = createAccount()
        jdbc.update(
            "UPDATE account SET default_tax_invoice_requested = TRUE WHERE public_id = ?",
            standardAccountId.toBytes(),
        )
        createCartWithItem(standardAccountId, skuId)
        val standardOrder = orders.create(standardAccountId, false, false)
        assertThat(standardOrder.taxInvoiceRequested).isFalse()
        assertThat(standardOrder.depositBankName).isEqualTo("Standard Bank")
        assertThat(standardOrder.depositAccountNumber).isEqualTo("111-222")
        assertThat(standardOrder.depositAccountHolder).isEqualTo("Standard Holder")
        assertPersistedOrderSnapshot(standardOrder.id, false, "Standard Bank", "111-222", "Standard Holder")
        assertThat(orders.checkoutOptions(standardAccountId).defaultTaxInvoiceRequested).isTrue()
    }

    private fun assertPersistedOrderSnapshot(
        orderId: UUID,
        taxInvoiceRequested: Boolean,
        bankName: String,
        accountNumber: String,
        accountHolder: String,
    ) {
        val row = jdbc.queryForMap(
            "SELECT tax_invoice_requested, deposit_bank_name, deposit_account_number, deposit_account_holder FROM purchase_order WHERE public_id = ?",
            orderId.toBytes(),
        )
        assertThat(row["tax_invoice_requested"]).isEqualTo(taxInvoiceRequested)
        assertThat(row["deposit_bank_name"]).isEqualTo(bankName)
        assertThat(row["deposit_account_number"]).isEqualTo(accountNumber)
        assertThat(row["deposit_account_holder"]).isEqualTo(accountHolder)
    }

    private fun createAccount(): UUID {
        val publicId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO account (public_id, login_id, password_hash, name, phone, status) VALUES (?, ?, 'test-hash', 'Checkout test', ?, 'ACTIVE')",
            publicId.toBytes(),
            "checkout-${UUID.randomUUID()}",
            "555${UUID.randomUUID().toString().take(7)}",
        )
        return publicId
    }

    private fun createCategory(): Long {
        val publicId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO category (public_id, name, path, depth, display_status) VALUES (?, 'Checkout test', ?, 0, 'VISIBLE')",
            publicId.toBytes(),
            "checkout-${UUID.randomUUID()}",
        )
        return jdbc.queryForObject("SELECT id FROM category WHERE public_id = ?", Long::class.java, publicId.toBytes())!!
    }

    private fun createProduct(categoryId: Long): Long {
        val publicId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO product (public_id, category_id, name, display_status, sales_status) VALUES (?, ?, 'Checkout test', 'VISIBLE', 'ON_SALE')",
            publicId.toBytes(),
            categoryId,
        )
        return jdbc.queryForObject("SELECT id FROM product WHERE public_id = ?", Long::class.java, publicId.toBytes())!!
    }

    private fun createSku(productId: Long): UUID {
        val publicId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO product_sku (public_id, product_id, sku_code, name, sale_price, sales_status) VALUES (?, ?, ?, 'Checkout test SKU', 2500, 'ON_SALE')",
            publicId.toBytes(),
            productId,
            "CHK-${UUID.randomUUID()}",
        )
        return publicId
    }

    private fun createStock(skuId: UUID) {
        val skuInternalId = jdbc.queryForObject("SELECT id FROM product_sku WHERE public_id = ?", Long::class.java, skuId.toBytes())!!
        jdbc.update(
            "INSERT INTO inventory_stock (sku_id, on_hand_quantity, reserved_quantity, safety_stock_quantity) VALUES (?, 10, 0, 0)",
            skuInternalId,
        )
    }

    private fun createCartWithItem(accountId: UUID, skuId: UUID) {
        val accountInternalId = jdbc.queryForObject("SELECT id FROM account WHERE public_id = ?", Long::class.java, accountId.toBytes())!!
        val skuInternalId = jdbc.queryForObject("SELECT id FROM product_sku WHERE public_id = ?", Long::class.java, skuId.toBytes())!!
        val cartCount = jdbc.queryForObject("SELECT COUNT(*) FROM cart WHERE account_id = ?", Int::class.java, accountInternalId)!!
        if (cartCount == 0) {
            jdbc.update("INSERT INTO cart (public_id, account_id) VALUES (?, ?)", UUID.randomUUID().toBytes(), accountInternalId)
        }
        val cartId = jdbc.queryForObject("SELECT id FROM cart WHERE account_id = ?", Long::class.java, accountInternalId)!!
        jdbc.update(
            "INSERT INTO cart_item (public_id, cart_id, sku_id, quantity) VALUES (?, ?, ?, 2)",
            UUID.randomUUID().toBytes(),
            cartId,
            skuInternalId,
        )
    }

    private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16).putLong(mostSignificantBits).putLong(leastSignificantBits).array()
}
