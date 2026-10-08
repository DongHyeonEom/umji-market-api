package com.buyeong.umji.api.order.integration

import com.buyeong.umji.api.account.integration.BusinessRegistrationVerificationJob
import com.buyeong.umji.api.account.integration.http.BusinessRegistrationStatusClient
import com.buyeong.umji.api.account.model.BusinessGroupRegistration
import com.buyeong.umji.api.account.model.BusinessRegistrationStatus
import com.buyeong.umji.api.account.model.OrganizationRegistrationCommand
import com.buyeong.umji.api.account.model.OrganizationTaxInvoiceProfileCommand
import com.buyeong.umji.api.account.model.SharedAddressCommand
import com.buyeong.umji.api.account.service.OrganizationMembershipService
import com.buyeong.umji.api.account.service.OrganizationTaxInvoiceProfileService
import com.buyeong.umji.api.account.service.CustomerAccountService
import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.operation.account.model.OrganizationProfileData
import com.buyeong.umji.api.operation.account.service.OperationAccountService
import com.buyeong.umji.api.order.service.OrderCancellationService
import com.buyeong.umji.api.order.service.OrderService
import com.buyeong.umji.api.payment.integration.TaxInvoiceSupplierService
import com.buyeong.umji.api.payment.service.PaymentService
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationJpaEntityService
import com.buyeong.umji.api.shipment.service.ShipmentService
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.nio.ByteBuffer
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.transaction.TestTransaction
import org.springframework.transaction.annotation.Transactional

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
        "umji.tax-invoice.supplier.business-registration-number=123-45-67890",
        "umji.tax-invoice.supplier.business-name=Umji Market",
        "umji.tax-invoice.supplier.representative-name=Seller",
        "umji.tax-invoice.supplier.business-address=Seoul",
        "umji.tax-invoice.supplier.business-industry=Wholesale",
        "umji.tax-invoice.supplier.business-item=Tools",
        "umji.tax-invoice.supplier.email=seller@example.com",
    ],
)
@ActiveProfiles("local")
@Transactional
@Rollback
class OrderCheckoutOptionsMySqlIntegrationTest {
    private data class SellerOrganizationFixture(val accountId: UUID, val internalId: Long, val publicId: UUID)

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var orders: OrderService

    @Autowired
    private lateinit var taxInvoiceSupplier: TaxInvoiceSupplierService

    @Autowired
    private lateinit var customerAccounts: CustomerAccountService

    @Autowired
    private lateinit var organizations: OrganizationJpaEntityService

    @Autowired
    private lateinit var groupMembership: OrganizationMembershipService

    @MockitoBean
    private lateinit var businessRegistrationStatus: BusinessRegistrationStatusClient

    @Autowired
    private lateinit var taxInvoiceProfiles: OrganizationTaxInvoiceProfileService

    @Autowired
    private lateinit var businessRegistrationVerificationJob: BusinessRegistrationVerificationJob

    @Autowired
    private lateinit var operationAccounts: OperationAccountService

    @Autowired
    private lateinit var payments: PaymentService

    @Autowired
    private lateinit var shipments: ShipmentService

    @Autowired
    private lateinit var cancellations: OrderCancellationService

    @Test
    fun `wholesale box count reserves base sku units and preserves ordered pack size`() {
        val accountId = createAccount()
        createBusinessGroup(accountId)
        val addressId = createAddress(accountId)
        val skuId = createSku(createProduct(createCategory()))
        createStock(skuId)
        createCartWithItem(accountId, skuId)
        val skuInternalId = jdbc.queryForObject("SELECT id FROM product_sku WHERE public_id = ?", Long::class.java, skuId.toBytes())!!
        jdbc.update(
            "UPDATE sales_offer SET units_per_sale = 12 WHERE product_sku_id = ? AND sales_channel_id = (SELECT id FROM sales_channel WHERE code = 'WHOLESALE')",
            skuInternalId,
        )
        jdbc.update("UPDATE inventory_stock SET on_hand_quantity = 100 WHERE sku_id = ?", skuInternalId)

        val order = orders.create(accountId, addressId, false, false).single()
        assertThat(order.items.single().quantity).isEqualTo(2)
        assertThat(order.items.single().unitsPerSale).isEqualTo(12)
        assertThat(
            jdbc.queryForObject("SELECT quantity FROM stock_reservation WHERE reservation_key = ?", Int::class.java, order.items.single().reservationKey.toBytes()),
        ).isEqualTo(24)

        jdbc.update(
            "UPDATE sales_offer SET units_per_sale = 6 WHERE product_sku_id = ? AND sales_channel_id = (SELECT id FROM sales_channel WHERE code = 'WHOLESALE')",
            skuInternalId,
        )
        assertThat(orders.detail(accountId, order.id).items.single().unitsPerSale).isEqualTo(12)
        shipments.prepareOrder(order.id)
        assertThat(jdbc.queryForObject("SELECT on_hand_quantity FROM inventory_stock WHERE sku_id = ?", Int::class.java, skuInternalId)).isEqualTo(76)
        assertThat(jdbc.queryForObject("SELECT reserved_quantity FROM inventory_stock WHERE sku_id = ?", Int::class.java, skuInternalId)).isZero()

        val cancelledAccountId = createAccount()
        createBusinessGroup(cancelledAccountId)
        val cancelledAddressId = createAddress(cancelledAccountId)
        createCartWithItem(cancelledAccountId, skuId)
        val pendingOrder = orders.create(cancelledAccountId, cancelledAddressId, false, false).single()
        assertThat(jdbc.queryForObject("SELECT reserved_quantity FROM inventory_stock WHERE sku_id = ?", Int::class.java, skuInternalId)).isEqualTo(12)
        cancellations.request(cancelledAccountId, pendingOrder.id)
        assertThat(jdbc.queryForObject("SELECT on_hand_quantity FROM inventory_stock WHERE sku_id = ?", Int::class.java, skuInternalId)).isEqualTo(76)
        assertThat(jdbc.queryForObject("SELECT reserved_quantity FROM inventory_stock WHERE sku_id = ?", Int::class.java, skuInternalId)).isZero()
    }

    @Test
    fun `multi seller checkout rolls back saved orders and earlier stock reservations when a later seller lacks stock`() {
        val buyerId = createAccount()
        val addressId = createAddress(buyerId)
        val categoryId = createCategory()
        val sellerA = createSellerOrganization("Rollback seller A")
        val sellerB = createSellerOrganization("Rollback seller B")
        val sellerASku = createSellerCatalogItem(categoryId, sellerA.internalId, 10)
        val sellerBSku = createSellerCatalogItem(categoryId, sellerB.internalId, 0)
        createCartWithItem(buyerId, sellerASku)
        createCartWithItem(buyerId, sellerBSku)

        entityManager.flush()
        TestTransaction.flagForCommit()
        TestTransaction.end()

        assertThatThrownBy { orders.create(buyerId, addressId, false, false) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("가용 재고가 부족합니다")

        TestTransaction.start()
        entityManager.clear()

        val buyerInternalId = jdbc.queryForObject(
            "SELECT id FROM account WHERE public_id = ?",
            Long::class.java,
            buyerId.toBytes(),
        )!!
        assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM purchase_order WHERE account_id = ?", Int::class.java, buyerInternalId),
        ).isZero()
        assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM cart_item item JOIN cart ON cart.id = item.cart_id WHERE cart.account_id = ?", Int::class.java, buyerInternalId),
        ).isEqualTo(2)
        assertSellerInventoryUnchanged(sellerA.internalId, sellerASku, 10)
        assertSellerInventoryUnchanged(sellerB.internalId, sellerBSku, 0)

        cleanupMultiSellerRollbackFixture(buyerId, categoryId, sellerA, sellerB)
        TestTransaction.flagForCommit()
        TestTransaction.end()
    }

    @Test
    fun `flyway v13 persists invoice preference and order-specific bank account snapshots`() {
        val migrationCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '13' AND success = TRUE",
            Int::class.java,
        )
        assertThat(migrationCount).isEqualTo(1)
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '33' AND success = TRUE",
                Int::class.java,
            ),
        ).isEqualTo(1)
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '34' AND success = TRUE",
                Int::class.java,
            ),
        ).isEqualTo(1)
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '24' AND success = TRUE",
                Int::class.java,
            ),
        ).isEqualTo(1)

        val accountId = createAccount()
        createBusinessGroup(accountId)
        taxInvoiceProfiles.updateForAccount(
            accountId,
            OrganizationTaxInvoiceProfileCommand(
                "987-65-43210", "Group test business", "Buyer", "12345", "Buyer address", null, "Retail", "Hardware", "buyer@example.com",
            ),
        )
        val addressId = createAddress(accountId)
        assertThat(customerAccounts.profile(accountId).id).isEqualTo(accountId)
        val categoryId = createCategory()
        val productId = createProduct(categoryId)
        val skuId = createSku(productId)
        createStock(skuId)
        createCartWithItem(accountId, skuId)

        val initialOptions = orders.checkoutOptions(accountId)
        assertThat(taxInvoiceSupplier.supplier()).isNotNull()
        val buyerProfile = taxInvoiceProfiles.forAccount(accountId)
        assertThat(buyerProfile.complete).withFailMessage("Expected complete buyer profile, got $buyerProfile").isTrue()
        assertThat(initialOptions.defaultTaxInvoiceRequested).isFalse()
        assertThat(initialOptions.standardBankAccount.bankName).isEqualTo("Standard Bank")
        assertThat(initialOptions.taxInvoiceBankAccount.bankName).isEqualTo("Tax Bank")
        assertThat(initialOptions.taxInvoiceAvailable).isTrue()
        assertThat(
            jdbc.queryForObject(
                "SELECT default_tax_invoice_requested FROM organization WHERE id = ?",
                Boolean::class.java,
                organizations.activeForAccountPublicId(accountId)?.id,
            ),
        ).isFalse()

        val invoiceOrder = orders.create(accountId, addressId, true, true).single()
        assertOutboxEvent(invoiceOrder.id, "ORDER_CREATED", null)
        assertThat(invoiceOrder.taxInvoiceRequested).isTrue()
        assertThat(invoiceOrder.depositBankName).isEqualTo("Tax Bank")
        assertThat(invoiceOrder.depositAccountNumber).isEqualTo("333-444")
        assertThat(invoiceOrder.depositAccountHolder).isEqualTo("Tax Holder")
        assertThat(invoiceOrder.shippingRecipientName).isEqualTo("Recipient")
        assertThat(invoiceOrder.shippingAddress1).isEqualTo("Seoul address")
        assertThat(invoiceOrder.taxInvoiceSnapshot?.status).isEqualTo("WAITING_FOR_SHIPMENT")
        assertThat(invoiceOrder.taxInvoiceSnapshot?.buyer?.businessName).isEqualTo("Group test business")
        assertPersistedOrderSnapshot(invoiceOrder.id, true, "Tax Bank", "333-444", "Tax Holder")
        customerAccounts.updateAddress(
            accountId,
            addressId,
            SharedAddressCommand("Changed recipient", "01087654321", "54321", "Changed address", null, true),
        )
        assertThat(orders.detail(accountId, invoiceOrder.id).shippingAddress1).isEqualTo("Seoul address")
        assertThat(orders.checkoutOptions(accountId).defaultTaxInvoiceRequested).isTrue()

        val standardAccountId = createAccount()
        val standardAddressId = createAddress(standardAccountId)
        jdbc.update(
            "UPDATE organization SET default_tax_invoice_requested = TRUE WHERE id = ?",
            organizations.activeForAccountPublicId(standardAccountId)?.id,
        )
        createCartWithItem(standardAccountId, skuId)
        val standardOrder = orders.create(standardAccountId, standardAddressId, false, false).single()
        assertOutboxEvent(standardOrder.id, "ORDER_CREATED", null)
        assertThat(standardOrder.taxInvoiceRequested).isFalse()
        assertThat(standardOrder.depositBankName).isEqualTo("Standard Bank")
        assertThat(standardOrder.depositAccountNumber).isEqualTo("111-222")
        assertThat(standardOrder.depositAccountHolder).isEqualTo("Standard Holder")
        assertPersistedOrderSnapshot(standardOrder.id, false, "Standard Bank", "111-222", "Standard Holder")
        assertThat(orders.checkoutOptions(standardAccountId).defaultTaxInvoiceRequested).isTrue()
    }

    @Test
    fun `order payment and shipment state transitions append one outbox event per changed state`() {
        val accountId = createAccount()
        val addressId = createAddress(accountId)
        val categoryId = createCategory()
        val productId = createProduct(categoryId)
        val skuId = createSku(productId)
        createStock(skuId)
        createCartWithItem(accountId, skuId)
        val order = orders.create(accountId, addressId, false, false).single()
        val operatorId = createAccount()

        payments.updateStatus(order.id, "PARTIAL_PAYMENT_REVIEW_REQUIRED", operatorId)
        payments.updateStatus(order.id, "PARTIAL_PAYMENT_REVIEW_REQUIRED", operatorId)
        shipments.prepareOrder(order.id)
        val trackingNumber = "TRACK-${UUID.randomUUID()}"
        shipments.registerTracking(order.id, "DAESIN", trackingNumber, operatorId)
        shipments.registerTracking(order.id, "DAESIN", trackingNumber, operatorId)
        shipments.markDelivered(order.id, operatorId)
        shipments.markDelivered(order.id, operatorId)

        assertOutboxEvent(order.id, "ORDER_CREATED", null)
        assertOutboxEvent(order.id, "PAYMENT_STATUS_CHANGED", "PARTIAL_PAYMENT_REVIEW_REQUIRED")
        assertOutboxEvent(order.id, "SHIPMENT_PREPARING", null)
        assertOutboxEvent(order.id, "SHIPMENT_IN_TRANSIT", null)
        assertOutboxEvent(order.id, "SHIPMENT_DELIVERED", null)

        val cancelledAccountId = createAccount()
        val cancelledAddressId = createAddress(cancelledAccountId)
        createCartWithItem(cancelledAccountId, skuId)
        val cancelledOrder = orders.create(cancelledAccountId, cancelledAddressId, false, false).single()
        cancellations.request(cancelledAccountId, cancelledOrder.id)
        assertOutboxEvent(cancelledOrder.id, "ORDER_CREATED", null)
        assertOutboxEvent(cancelledOrder.id, "ORDER_CANCELLED", null)
    }

    @Test
    fun `business group members share addresses while other groups cannot access them`() {
        val ownerId = createAccount()
        val memberId = createAccount()
        val otherId = createAccount()
        val businessGroupId = createBusinessGroup(ownerId)
        organizations.ensureForAccount(memberId)
        organizations.assignAccountToOrganization(memberId, businessGroupId)
        organizations.ensureForAccount(otherId)

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

    @Test
    fun `group members share orders with masked orderer details while other groups stay isolated`() {
        val ownerId = createAccount()
        val memberId = createAccount()
        val otherId = createAccount()
        val businessGroupId = createBusinessGroup(ownerId)
        organizations.ensureForAccount(memberId)
        organizations.assignAccountToOrganization(memberId, businessGroupId)
        val otherGroupId = createBusinessGroup(otherId)
        jdbc.update(
            "UPDATE organization_business_profile SET business_registration_number = NULL WHERE organization_id = (SELECT id FROM organization WHERE public_id = ?)",
            businessGroupId.toBytes(),
        )

        assertThat(jdbc.queryForObject("SELECT organization_type FROM organization WHERE public_id = ?", String::class.java, businessGroupId.toBytes()))
            .isEqualTo("BUSINESS")
        assertThat(
            jdbc.queryForObject(
                "SELECT business_registration_number IS NULL FROM organization_business_profile WHERE organization_id = (SELECT id FROM organization WHERE public_id = ?)",
                Boolean::class.java,
                businessGroupId.toBytes(),
            ),
        ).isTrue()
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM organization_member WHERE organization_id = (SELECT id FROM organization WHERE public_id = ?) AND status = 'ACTIVE'",
                Int::class.java,
                businessGroupId.toBytes(),
            ),
        ).isEqualTo(2)
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM organization_member WHERE account_id = (SELECT id FROM account WHERE public_id = ?) AND status = 'ACTIVE'",
                Int::class.java,
                memberId.toBytes(),
            ),
        ).isEqualTo(1)

        val addressId = createAddress(ownerId)
        val categoryId = createCategory()
        val skuId = createSku(createProduct(categoryId))
        createStock(skuId)

        createCartWithItem(ownerId, skuId)
        val ownerOrder = orders.create(ownerId, addressId, false, false).single()
        createCartWithItem(memberId, skuId)
        val memberOrder = orders.create(memberId, addressId, false, false).single()

        assertThat(orders.list(ownerId, 0, 20).items.map { it.id }).containsExactlyInAnyOrder(ownerOrder.id, memberOrder.id)
        assertThat(orders.list(memberId, 0, 20).items.map { it.id }).containsExactlyInAnyOrder(ownerOrder.id, memberOrder.id)
        assertThat(orders.detail(ownerId, memberOrder.id).orderedByName).matches("C\\*+")
        assertThat(orders.detail(ownerId, memberOrder.id).orderedByPhoneSuffix).matches("\\d{4}")
        assertThat(orders.list(otherId, 0, 20).items).isEmpty()
        assertThatThrownBy { orders.detail(otherId, memberOrder.id) }.isInstanceOf(ItemNotFoundException::class.java)

        organizations.assignAccountToOrganization(memberId, otherGroupId)
        assertThat(orders.list(memberId, 0, 20).items).isEmpty()
        assertThat(orders.list(ownerId, 0, 20).items.map { it.id }).containsExactlyInAnyOrder(ownerOrder.id, memberOrder.id)
    }

    @Test
    fun `group invitations and join requests require recipient acceptance and representative approval`() {
        val representativeId = createAccount()
        val invitedId = createAccount()
        val requestedId = createAccount()
        val businessGroupId = createBusinessGroup(representativeId)
        organizations.ensureForAccount(invitedId)
        organizations.ensureForAccount(requestedId)

        groupMembership.invite(representativeId, accountPhone(invitedId))
        assertThat(groupMembership.invitations(invitedId)).hasSize(1)
        groupMembership.respondInvitation(invitedId, groupMembership.invitations(invitedId).single().id, true)
        assertThat(groupMembership.current(invitedId)?.id).isEqualTo(businessGroupId)
        assertThat(jdbc.queryForObject("SELECT status FROM organization WHERE public_id = ?", String::class.java, personalGroupId(invitedId).toBytes()))
            .isEqualTo("INACTIVE")

        groupMembership.requestToJoin(requestedId, businessGroupId)
        val pending = groupMembership.pendingJoinRequests(representativeId).single()
        assertThat(pending.requesterPhone).endsWith(accountPhone(requestedId).takeLast(4))
        groupMembership.respondJoinRequest(representativeId, pending.id, true)
        assertThat(groupMembership.current(requestedId)?.id).isEqualTo(businessGroupId)
        assertThat(groupMembership.search("010-9000-0000").map { it.id }).contains(businessGroupId)
        assertThat(groupMembership.search(accountPhone(requestedId)).map { it.id }).contains(businessGroupId)
    }

    @Test
    fun `business groups with the same registration number are not merged`() {
        val firstRepresentative = createAccount()
        val secondRepresentative = createAccount()
        val firstGroupId = createBusinessGroup(firstRepresentative)
        val secondGroupId = createBusinessGroup(secondRepresentative)

        assertThat(firstGroupId).isNotEqualTo(secondGroupId)
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(DISTINCT organization_id) FROM organization_business_profile WHERE business_registration_number = '987-65-43210'",
                Int::class.java,
            ),
        ).isGreaterThanOrEqualTo(2)
        assertThat(groupMembership.current(firstRepresentative)?.id).isEqualTo(firstGroupId)
        assertThat(groupMembership.current(secondRepresentative)?.id).isEqualTo(secondGroupId)
    }

    @Test
    fun `only the configured representative can invite members and review join requests`() {
        val formerRepresentative = createAccount()
        val memberId = createAccount()
        val applicantId = createAccount()
        val businessGroupId = createBusinessGroup(formerRepresentative)
        organizations.ensureForAccount(memberId)
        organizations.assignAccountToOrganization(memberId, businessGroupId)
        organizations.ensureForAccount(applicantId)

        operationAccounts.setOrganizationRepresentative(businessGroupId, memberId)
        assertThat(groupMembership.pendingJoinRequests(formerRepresentative)).isEmpty()
        assertThatThrownBy { groupMembership.invite(formerRepresentative, accountPhone(applicantId)) }
            .isInstanceOf(ItemNotFoundException::class.java)

        groupMembership.requestToJoin(applicantId, businessGroupId)
        val pending = groupMembership.pendingJoinRequests(memberId).single()
        assertThatThrownBy { groupMembership.respondJoinRequest(formerRepresentative, pending.id, true) }
            .isInstanceOf(IllegalArgumentException::class.java)
        groupMembership.respondJoinRequest(memberId, pending.id, false)
        assertThat(groupMembership.pendingJoinRequests(memberId)).isEmpty()
        assertThat(groupMembership.current(applicantId)?.id).isNotEqualTo(businessGroupId)
    }

    @Test
    fun `ungrouped first-time account can create an individual group and becomes its representative`() {
        val accountId = createAccount()

        assertThat(groupMembership.current(accountId)).isNull()
        val group = groupMembership.createIndividualGroup(accountId, "Personal wholesale")

        assertThat(group.type).isEqualTo("INDIVIDUAL")
        assertThat(group.representative).isTrue()
        assertThat(groupMembership.current(accountId)?.id).isEqualTo(group.id)
    }

    private fun assertPersistedOrderSnapshot(
        orderId: UUID,
        taxInvoiceRequested: Boolean,
        bankName: String,
        accountNumber: String,
        accountHolder: String,
    ) {
        val row = jdbc.queryForMap(
            "SELECT EXISTS (SELECT 1 FROM purchase_order_tax_invoice invoice WHERE invoice.order_id = purchase_order.id) AS tax_invoice_requested, deposit_bank_name, deposit_account_number, deposit_account_holder, shipping_recipient_name, shipping_recipient_phone, shipping_postal_code, shipping_address1, shipping_address2 FROM purchase_order WHERE public_id = ?",
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

    private fun assertOutboxEvent(orderId: UUID, eventType: String, detail: String?) {
        val matchingRows = jdbc.queryForList(
            "SELECT event_detail FROM notification_outbox WHERE order_public_id = ? AND event_type = ?",
            orderId.toBytes(),
            eventType,
        )
        assertThat(matchingRows).hasSize(1)
        assertThat(matchingRows.single()["event_detail"]).isEqualTo(detail)
    }

    private fun createAddress(accountId: UUID): UUID {
        organizations.ensureForAccount(accountId)
        return customerAccounts.createAddress(
            accountId,
            SharedAddressCommand("Recipient", "01012345678", "12345", "Seoul address", "Details", false),
        ).id
    }

    private fun createBusinessGroup(accountId: UUID, verified: Boolean = true): UUID {
        val group = organizations.ensureForAccount(
            accountId,
            profileData = com.buyeong.umji.api.operation.account.model.OrganizationProfileData(
                businessName = "Group test business",
                businessRegistrationNumber = null,
                representativeName = "Buyer",
                businessPhone = "010-9000-0000",
                postalCode = "12345",
                address1 = "Registered Place",
                address2 = null,
                status = "ACTIVE",
            ),
        )
        if (verified) {
            jdbc.update(
                "UPDATE organization_business_profile SET business_registration_number = '987-65-43210', business_registration_verification_status = 'ACTIVE', business_registration_verified_at = CURRENT_TIMESTAMP(3), business_registration_confirmed_at = CURRENT_TIMESTAMP(3) WHERE organization_id = ?",
                group.id,
            )
        } else {
            jdbc.update(
                "UPDATE organization_business_profile SET business_registration_number = '987-65-43210' WHERE organization_id = ?",
                group.id,
            )
        }
        entityManager.clear()
        return requireNotNull(group.publicId)
    }

    @Test
    fun `buyer group tax invoice profile is shared and order snapshot becomes issuable on shipment start`() {
        val ownerId = createAccount()
        val organizationId = createBusinessGroup(ownerId)
        val command = OrganizationTaxInvoiceProfileCommand(
            "987-65-43210", "Group test business", "Buyer", "12345", "Buyer address", "Suite 2", "Retail", "Hardware", "buyer@example.com",
        )
        val saved = taxInvoiceProfiles.updateForAccount(ownerId, command)
        assertThat(saved.complete).isTrue()
        assertThat(taxInvoiceProfiles.forGroup(organizationId).email).isEqualTo("buyer@example.com")

        val addressId = createAddress(ownerId)
        val categoryId = createCategory()
        val productId = createProduct(categoryId)
        val skuId = createSku(productId)
        createStock(skuId)
        createCartWithItem(ownerId, skuId)
        val order = orders.create(ownerId, addressId, true, false).single()
        assertThat(order.taxInvoiceSnapshot?.status).isEqualTo("WAITING_FOR_SHIPMENT")
        assertThat(order.taxInvoiceSnapshot?.writtenDate).isNull()
        assertThat(order.taxInvoiceSnapshot?.supplyAmount).isEqualTo(order.items.sumOf { it.lineAmount })

        val operatorId = createAccount()
        shipments.prepareOrder(order.id)
        shipments.registerTracking(order.id, "DAESIN", "INVOICE-${UUID.randomUUID()}", operatorId)
        val ready = orders.detail(ownerId, order.id).taxInvoiceSnapshot
        assertThat(ready?.status).isEqualTo("READY_FOR_ISSUANCE")
        assertThat(ready?.writtenDate).isEqualTo(order.orderedAt.atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate())
        assertThat(ready?.supplyDate).isEqualTo(ready?.writtenDate)

        jdbc.update(
            "UPDATE organization_business_profile SET business_name = 'Changed later' WHERE organization_id = (SELECT id FROM organization WHERE public_id = ?)",
            organizationId.toBytes(),
        )
        assertThat(orders.detail(ownerId, order.id).taxInvoiceSnapshot?.buyer?.businessName).isEqualTo("Group test business")

        val memberId = createAccount()
        organizations.ensureForAccount(memberId)
        organizations.assignAccountToOrganization(memberId, organizationId)
        assertThatThrownBy { taxInvoiceProfiles.updateForAccount(memberId, command) }
            .isInstanceOf(com.buyeong.umji.api.exception.ForbiddenOperationException::class.java)
    }

    @Test
    fun `pre-registered group waits for background verification then representative confirmation`() {
        val ownerId = createAccount()
        createBusinessGroup(ownerId, verified = false)
        val command = OrganizationTaxInvoiceProfileCommand(
            "987-65-43210", "Group test business", "Buyer", "12345", "Buyer address", null, "Retail", "Hardware", "buyer@example.com",
        )

        assertThat(taxInvoiceProfiles.forAccount(ownerId).businessRegistrationVerificationStatus).isEqualTo("PENDING")
        assertThatThrownBy { taxInvoiceProfiles.updateForAccount(ownerId, command) }
            .isInstanceOf(IllegalStateException::class.java)

        Mockito.`when`(businessRegistrationStatus.lookup("987-65-43210"))
            .thenReturn(BusinessRegistrationStatus.ACTIVE)
        businessRegistrationVerificationJob.verifyPendingProfiles()

        val waitingForConfirmation = taxInvoiceProfiles.forAccount(ownerId)
        assertThat(waitingForConfirmation.businessRegistrationVerificationStatus).isEqualTo("ACTIVE")
        assertThat(waitingForConfirmation.complete).isFalse()
        assertThatThrownBy {
            taxInvoiceProfiles.updateForAccount(ownerId, command.copy(businessRegistrationNumber = "0000000000"))
        }.isInstanceOf(IllegalArgumentException::class.java)
        assertThat(taxInvoiceProfiles.updateForAccount(ownerId, command).complete).isTrue()
    }

    @Test
    fun `first group registration creates personal group or verifies business number and keeps registered address separate`() {
        val personalAccountId = createAccount()
        val personalGroup = groupMembership.register(personalAccountId, OrganizationRegistrationCommand("INDIVIDUAL", null))
        assertThat(personalGroup.type).isEqualTo("INDIVIDUAL")
        assertThat(groupMembership.current(personalAccountId)?.id).isEqualTo(personalGroup.id)
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM organization_business_profile WHERE organization_id = (SELECT id FROM organization WHERE public_id = ?)",
                Int::class.java,
                personalGroup.id.toBytes(),
            ),
        ).isZero()

        val businessAccountId = createAccount()
        Mockito.doNothing().`when`(businessRegistrationStatus).ensureNotClosed("1234567890")
        val businessGroup = groupMembership.register(
            businessAccountId,
            OrganizationRegistrationCommand(
                "BUSINESS",
                BusinessGroupRegistration(
                    "1234567890", "Buyer Business", "Buyer Owner", "12345", "Registered Place", "Building 1", "Retail", "Hardware", "billing@example.com", true,
                ),
            ),
        )
        assertThat(businessGroup.type).isEqualTo("BUSINESS")
        Mockito.verify(businessRegistrationStatus).ensureNotClosed("1234567890")
        assertThat(
            jdbc.queryForObject(
                "SELECT business_registration_verified_at IS NOT NULL FROM organization_business_profile WHERE organization_id = (SELECT id FROM organization WHERE public_id = ?)",
                Boolean::class.java,
                businessGroup.id.toBytes(),
            ),
        ).isTrue()
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM organization_address WHERE organization_id = (SELECT id FROM organization WHERE public_id = ?)",
                Int::class.java,
                businessGroup.id.toBytes(),
            ),
        ).isZero()
        assertThatThrownBy { groupMembership.register(businessAccountId, OrganizationRegistrationCommand("INDIVIDUAL", null)) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `closed business registration is rejected before group creation and preset group skips external check`() {
        val closedAccountId = createAccount()
        Mockito.doThrow(ClientBadRequestException("폐업 상태"))
            .`when`(businessRegistrationStatus).ensureNotClosed("1234567890")
        assertThatThrownBy {
            groupMembership.register(
                closedAccountId,
                OrganizationRegistrationCommand(
                    "BUSINESS",
                    BusinessGroupRegistration(
                        "1234567890", "Closed Business", "Owner", "12345", "Registered Place", null, "Retail", "Hardware", null, true,
                    ),
                ),
            )
        }.isInstanceOf(ClientBadRequestException::class.java)
        assertThat(groupMembership.current(closedAccountId)).isNull()

        val presetAccountId = createAccount()
        createBusinessGroup(presetAccountId)
        Mockito.verify(businessRegistrationStatus, Mockito.times(1)).ensureNotClosed("1234567890")
    }

    private fun createAccount(): UUID {
        val publicId = UUID.randomUUID()
        val phone = "010${UUID.randomUUID().toString().filter(Char::isDigit).padEnd(8, '0').take(8)}"
        jdbc.update(
            "INSERT INTO account (public_id, login_id, password_hash, name, phone, phone_normalized, status) VALUES (?, ?, 'test-hash', 'Checkout test', ?, ?, 'ACTIVE')",
            publicId.toBytes(),
            "checkout-${UUID.randomUUID()}",
            phone,
            phone,
        )
        return publicId
    }

    private fun accountPhone(accountId: UUID): String =
        jdbc.queryForObject("SELECT phone_normalized FROM account WHERE public_id = ?", String::class.java, accountId.toBytes())!!

    private fun personalGroupId(accountId: UUID): UUID {
        val groupBytes = jdbc.queryForObject(
            "SELECT organization.public_id FROM organization JOIN organization_member ON organization_member.organization_id = organization.id WHERE organization_member.account_id = (SELECT id FROM account WHERE public_id = ?) AND organization.organization_type = 'INDIVIDUAL'",
            ByteArray::class.java,
            accountId.toBytes(),
        )!!
        return ByteBuffer.wrap(groupBytes).let { UUID(it.long, it.long) }
    }

    private fun createCategory(): Long {
        val publicId = UUID.randomUUID()
        val channelId = jdbc.queryForObject("SELECT id FROM sales_channel WHERE code = 'WHOLESALE'", Long::class.java)!!
        jdbc.update(
            "INSERT INTO category (public_id, sales_channel_id, name, path, depth, display_status) VALUES (?, ?, 'Checkout test', ?, 0, 'VISIBLE')",
            publicId.toBytes(),
            channelId,
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
        val skuInternalId = jdbc.queryForObject("SELECT id FROM product_sku WHERE public_id = ?", Long::class.java, publicId.toBytes())!!
        val channelId = jdbc.queryForObject("SELECT id FROM sales_channel WHERE code = 'WHOLESALE'", Long::class.java)!!
        jdbc.update(
            "INSERT INTO sales_offer (public_id, sales_channel_id, product_sku_id, sale_price, sales_status) VALUES (?, ?, ?, 2500, 'ON_SALE')",
            UUID.randomUUID().toBytes(),
            channelId,
            skuInternalId,
        )
        return publicId
    }

    private fun createSellerOrganization(businessName: String): SellerOrganizationFixture {
        val accountId = createAccount()
        val registrationNumber = if (businessName.endsWith("A")) "9876543210" else "9876543211"
        val organization = organizations.ensureForAccount(
            accountId,
            capability = "SELLER",
            profileData = OrganizationProfileData(
                businessName = businessName,
                businessRegistrationNumber = registrationNumber,
                representativeName = "Seller representative",
                businessPhone = "010-9000-0000",
                postalCode = "12345",
                address1 = "Seller address",
                address2 = null,
                status = "COMPLETED",
            ),
        )
        jdbc.update(
            "UPDATE organization_business_profile SET business_registration_verification_status = 'ACTIVE', business_registration_verified_at = CURRENT_TIMESTAMP(3) WHERE organization_id = ?",
            organization.id,
        )
        entityManager.clear()
        taxInvoiceProfiles.updateForAccount(
            accountId,
            OrganizationTaxInvoiceProfileCommand(
                registrationNumber,
                businessName,
                "Seller representative",
                "12345",
                "Seller address",
                null,
                "Wholesale",
                "Tools",
                "${businessName.lowercase().replace(' ', '.')}@example.com",
            ),
        )
        return SellerOrganizationFixture(accountId, requireNotNull(organization.id), requireNotNull(organization.publicId))
    }

    private fun createSellerCatalogItem(categoryId: Long, sellerId: Long, stockQuantity: Int): UUID {
        val productPublicId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO product (public_id, organization_id, category_id, name, display_status, sales_status) VALUES (?, ?, ?, ?, 'VISIBLE', 'ON_SALE')",
            productPublicId.toBytes(),
            sellerId,
            categoryId,
            "Rollback product ${UUID.randomUUID()}",
        )
        val productId = jdbc.queryForObject("SELECT id FROM product WHERE public_id = ?", Long::class.java, productPublicId.toBytes())!!
        val skuPublicId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO product_sku (public_id, product_id, sku_code, name, sale_price, sales_status) VALUES (?, ?, ?, 'Rollback SKU', 2500, 'ON_SALE')",
            skuPublicId.toBytes(),
            productId,
            "RB-${UUID.randomUUID()}",
        )
        val skuId = jdbc.queryForObject("SELECT id FROM product_sku WHERE public_id = ?", Long::class.java, skuPublicId.toBytes())!!
        val channelId = jdbc.queryForObject("SELECT id FROM sales_channel WHERE code = 'WHOLESALE'", Long::class.java)!!
        jdbc.update(
            "INSERT INTO sales_offer (public_id, sales_channel_id, organization_id, product_sku_id, sale_price, sales_status) VALUES (?, ?, ?, ?, 2500, 'ON_SALE')",
            UUID.randomUUID().toBytes(),
            channelId,
            sellerId,
            skuId,
        )
        jdbc.update(
            "INSERT INTO inventory_stock (organization_id, sku_id, on_hand_quantity, reserved_quantity, safety_stock_quantity) VALUES (?, ?, ?, 0, 0)",
            sellerId,
            skuId,
            stockQuantity,
        )
        return skuPublicId
    }

    private fun assertSellerInventoryUnchanged(sellerId: Long, skuPublicId: UUID, expectedOnHand: Int) {
        val skuId = jdbc.queryForObject("SELECT id FROM product_sku WHERE public_id = ?", Long::class.java, skuPublicId.toBytes())!!
        assertThat(
            jdbc.queryForObject(
                "SELECT on_hand_quantity FROM inventory_stock WHERE organization_id = ? AND sku_id = ?",
                Int::class.java,
                sellerId,
                skuId,
            ),
        ).isEqualTo(expectedOnHand)
        assertThat(
            jdbc.queryForObject(
                "SELECT reserved_quantity FROM inventory_stock WHERE organization_id = ? AND sku_id = ?",
                Int::class.java,
                sellerId,
                skuId,
            ),
        ).isZero()
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_reservation WHERE organization_id = ? AND sku_id = ?",
                Int::class.java,
                sellerId,
                skuId,
            ),
        ).isZero()
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM inventory_movement WHERE organization_id = ? AND sku_id = ?",
                Int::class.java,
                sellerId,
                skuId,
            ),
        ).isZero()
    }

    private fun cleanupMultiSellerRollbackFixture(
        buyerId: UUID,
        categoryId: Long,
        sellerA: SellerOrganizationFixture,
        sellerB: SellerOrganizationFixture,
    ) {
        val buyerOrganizationId = jdbc.queryForObject(
            "SELECT organization_id FROM organization_member WHERE account_id = (SELECT id FROM account WHERE public_id = ?) AND status = 'ACTIVE'",
            Long::class.java,
            buyerId.toBytes(),
        )!!
        val organizationIds = listOf(buyerOrganizationId, sellerA.internalId, sellerB.internalId)
        jdbc.update("DELETE FROM cart_item WHERE cart_id IN (SELECT id FROM cart WHERE account_id = (SELECT id FROM account WHERE public_id = ?))", buyerId.toBytes())
        jdbc.update("DELETE FROM cart WHERE account_id = (SELECT id FROM account WHERE public_id = ?)", buyerId.toBytes())
        organizationIds.forEach { organizationId ->
            jdbc.update("DELETE FROM organization_address WHERE organization_id = ?", organizationId)
            jdbc.update("DELETE FROM organization_business_profile WHERE organization_id = ?", organizationId)
            jdbc.update("DELETE FROM organization_capability WHERE organization_id = ?", organizationId)
            jdbc.update("DELETE FROM organization_member WHERE organization_id = ?", organizationId)
        }
        listOf(sellerA.internalId, sellerB.internalId).forEach { sellerId ->
            jdbc.update("DELETE FROM inventory_movement WHERE organization_id = ?", sellerId)
            jdbc.update("DELETE FROM stock_reservation WHERE organization_id = ?", sellerId)
            jdbc.update("DELETE FROM inventory_stock WHERE organization_id = ?", sellerId)
            jdbc.update("DELETE FROM sales_offer WHERE organization_id = ?", sellerId)
            jdbc.update("DELETE FROM product_sku WHERE product_id IN (SELECT id FROM product WHERE organization_id = ?)", sellerId)
            jdbc.update("DELETE FROM product WHERE organization_id = ?", sellerId)
        }
        jdbc.update("DELETE FROM category WHERE id = ?", categoryId)
        organizationIds.forEach { organizationId -> jdbc.update("DELETE FROM organization WHERE id = ?", organizationId) }
        (listOf(buyerId, sellerA.accountId, sellerB.accountId)).forEach { accountId ->
            jdbc.update("DELETE FROM account WHERE public_id = ?", accountId.toBytes())
        }
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
        val offerId = jdbc.queryForObject(
            "SELECT offer.id FROM sales_offer offer JOIN sales_channel channel ON channel.id = offer.sales_channel_id WHERE offer.product_sku_id = ? AND channel.code = 'WHOLESALE'",
            Long::class.java,
            skuInternalId,
        )!!
        jdbc.update(
            "INSERT INTO cart_item (public_id, cart_id, sku_id, sales_offer_id, quantity) VALUES (?, ?, ?, ?, 2)",
            UUID.randomUUID().toBytes(),
            cartId,
            skuInternalId,
            offerId,
        )
    }

    private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16).putLong(mostSignificantBits).putLong(leastSignificantBits).array()
}
