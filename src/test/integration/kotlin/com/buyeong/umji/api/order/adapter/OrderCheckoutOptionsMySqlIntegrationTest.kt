package com.buyeong.umji.api.order.adapter

import com.buyeong.umji.api.account.application.model.SharedAddressCommand
import com.buyeong.umji.api.account.application.port.`in`.CustomerAccountUseCase
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.order.application.port.`in`.OrderUseCase
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupJpaEntityService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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

    @Autowired
    private lateinit var customerAccounts: CustomerAccountUseCase

    @Autowired
    private lateinit var buyerGroups: BuyerGroupJpaEntityService

    @Test
    fun `flyway v13 persists invoice preference and order-specific bank account snapshots`() {
        val migrationCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '13' AND success = TRUE",
            Int::class.java,
        )
        assertThat(migrationCount).isEqualTo(1)

        val accountId = createAccount()
        val addressId = createAddress(accountId)
        assertThat(customerAccounts.profile(accountId).id).isEqualTo(accountId)
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

        val invoiceOrder = orders.create(accountId, addressId, true, true)
        assertThat(invoiceOrder.taxInvoiceRequested).isTrue()
        assertThat(invoiceOrder.depositBankName).isEqualTo("Tax Bank")
        assertThat(invoiceOrder.depositAccountNumber).isEqualTo("333-444")
        assertThat(invoiceOrder.depositAccountHolder).isEqualTo("Tax Holder")
        assertThat(invoiceOrder.shippingRecipientName).isEqualTo("Recipient")
        assertThat(invoiceOrder.shippingAddress1).isEqualTo("Seoul address")
        assertPersistedOrderSnapshot(invoiceOrder.id, true, "Tax Bank", "333-444", "Tax Holder")
        customerAccounts.updateAddress(
            accountId,
            addressId,
            SharedAddressCommand("Changed recipient", "01087654321", "54321", "Changed address", null, true),
        )
        assertThat(orders.detail(accountId, invoiceOrder.id).shippingAddress1).isEqualTo("Seoul address")
        assertThat(orders.checkoutOptions(accountId).defaultTaxInvoiceRequested).isTrue()

        val standardAccountId = createAccount()
        jdbc.update(
            "UPDATE account SET default_tax_invoice_requested = TRUE WHERE public_id = ?",
            standardAccountId.toBytes(),
        )
        val standardAddressId = createAddress(standardAccountId)
        createCartWithItem(standardAccountId, skuId)
        val standardOrder = orders.create(standardAccountId, standardAddressId, false, false)
        assertThat(standardOrder.taxInvoiceRequested).isFalse()
        assertThat(standardOrder.depositBankName).isEqualTo("Standard Bank")
        assertThat(standardOrder.depositAccountNumber).isEqualTo("111-222")
        assertThat(standardOrder.depositAccountHolder).isEqualTo("Standard Holder")
        assertPersistedOrderSnapshot(standardOrder.id, false, "Standard Bank", "111-222", "Standard Holder")
        assertThat(orders.checkoutOptions(standardAccountId).defaultTaxInvoiceRequested).isTrue()
    }

    @Test
    fun `business group members share addresses while other groups cannot access them`() {
        val ownerId = createAccount()
        val memberId = createAccount()
        val otherId = createAccount()
        val businessGroupId = createBusinessGroup(ownerId)
        buyerGroups.ensureForAccount(memberId)
        buyerGroups.assignAccountToBusinessGroup(memberId, businessGroupId)
        buyerGroups.ensureForAccount(otherId)

        val sharedAddress = customerAccounts.createAddress(
            ownerId,
            SharedAddressCommand("Group recipient", "01012345678", "12345", "Shared address", null, false),
        )

        assertThat(sharedAddress.isDefault).isTrue()
        assertThat(customerAccounts.addresses(memberId)).hasSize(1)
        assertThat(customerAccounts.addresses(memberId).single().id).isEqualTo(sharedAddress.id)
        assertThat(customerAccounts.addresses(otherId)).isEmpty()
        assertThatThrownBy { orders.create(otherId, sharedAddress.id, false, false) }
            .isInstanceOf(ItemNotFoundException::class.java)

        val secondAddress = customerAccounts.createAddress(
            memberId,
            SharedAddressCommand("Second recipient", "01087654321", "54321", "Second address", null, false),
        )
        customerAccounts.updateAddress(
            memberId,
            sharedAddress.id,
            SharedAddressCommand("Updated recipient", "01012345678", "12345", "Updated shared address", null, false),
        )
        assertThat(customerAccounts.addresses(ownerId).first { it.id == sharedAddress.id }.recipientName).isEqualTo("Updated recipient")

        customerAccounts.deleteAddress(memberId, sharedAddress.id)
        val remainingAddress = customerAccounts.addresses(ownerId).single()
        assertThat(remainingAddress.id).isEqualTo(secondAddress.id)
        assertThat(remainingAddress.isDefault).isTrue()
        assertThatThrownBy {
            customerAccounts.updateAddress(
                otherId,
                secondAddress.id,
                SharedAddressCommand("No access", "01011112222", "10000", "Hidden", null, false),
            )
        }.isInstanceOf(ItemNotFoundException::class.java)
    }

    private fun assertPersistedOrderSnapshot(
        orderId: UUID,
        taxInvoiceRequested: Boolean,
        bankName: String,
        accountNumber: String,
        accountHolder: String,
    ) {
        val row = jdbc.queryForMap(
            "SELECT tax_invoice_requested, deposit_bank_name, deposit_account_number, deposit_account_holder, shipping_recipient_name, shipping_recipient_phone, shipping_postal_code, shipping_address1, shipping_address2 FROM purchase_order WHERE public_id = ?",
            orderId.toBytes(),
        )
        assertThat(row["tax_invoice_requested"]).isEqualTo(taxInvoiceRequested)
        assertThat(row["deposit_bank_name"]).isEqualTo(bankName)
        assertThat(row["deposit_account_number"]).isEqualTo(accountNumber)
        assertThat(row["deposit_account_holder"]).isEqualTo(accountHolder)
        assertThat(row["shipping_recipient_name"]).isEqualTo("Recipient")
        assertThat(row["shipping_recipient_phone"]).isEqualTo("01012345678")
        assertThat(row["shipping_postal_code"]).isEqualTo("12345")
        assertThat(row["shipping_address1"]).isEqualTo("Seoul address")
        assertThat(row["shipping_address2"]).isEqualTo("Details")
    }

    private fun createAddress(accountId: UUID): UUID {
        buyerGroups.ensureForAccount(accountId)
        return customerAccounts.createAddress(
            accountId,
            SharedAddressCommand("Recipient", "01012345678", "12345", "Seoul address", "Details", false),
        ).id
    }

    private fun createBusinessGroup(accountId: UUID): UUID {
        val accountInternalId = jdbc.queryForObject("SELECT id FROM account WHERE public_id = ?", Long::class.java, accountId.toBytes())!!
        jdbc.update(
            "INSERT INTO business_profile (account_id, business_name, status) VALUES (?, 'Group test business', 'ACTIVE')",
            accountInternalId,
        )
        return requireNotNull(buyerGroups.ensureForAccount(accountId).publicId)
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