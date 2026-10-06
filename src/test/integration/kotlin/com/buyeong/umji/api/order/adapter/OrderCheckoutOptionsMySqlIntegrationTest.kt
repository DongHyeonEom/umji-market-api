package com.buyeong.umji.api.order.adapter

import com.buyeong.umji.api.account.adapter.BusinessRegistrationVerificationJob
import com.buyeong.umji.api.account.application.model.BuyerGroupRegistrationCommand
import com.buyeong.umji.api.account.application.model.BuyerGroupTaxInvoiceProfileCommand
import com.buyeong.umji.api.account.application.model.BusinessGroupRegistration
import com.buyeong.umji.api.account.application.model.SharedAddressCommand
import com.buyeong.umji.api.account.application.port.`in`.BuyerGroupMembershipUseCase
import com.buyeong.umji.api.account.application.port.`in`.BuyerGroupTaxInvoiceProfileUseCase
import com.buyeong.umji.api.account.application.port.`in`.CustomerAccountUseCase
import com.buyeong.umji.api.account.application.port.out.BusinessRegistrationStatus
import com.buyeong.umji.api.account.application.port.out.BusinessRegistrationStatusPort
import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.operation.account.application.port.`in`.OperationAccountUseCase
import com.buyeong.umji.api.operation.payment.adapter.`in`.web.TransactionalPaymentUseCase
import com.buyeong.umji.api.operation.shipment.adapter.`in`.web.TransactionalShipmentUseCase
import com.buyeong.umji.api.order.adapter.`in`.web.TransactionalOrderCancellationUseCase
import com.buyeong.umji.api.order.application.port.`in`.OrderUseCase
import com.buyeong.umji.api.order.application.port.out.TaxInvoiceSupplierPort
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupJpaEntityService
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
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
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var orders: OrderUseCase

    @Autowired
    private lateinit var taxInvoiceSupplier: TaxInvoiceSupplierPort

    @Autowired
    private lateinit var customerAccounts: CustomerAccountUseCase

    @Autowired
    private lateinit var buyerGroups: BuyerGroupJpaEntityService

    @Autowired
    private lateinit var groupMembership: BuyerGroupMembershipUseCase

    @MockitoBean
    private lateinit var businessRegistrationStatus: BusinessRegistrationStatusPort

    @Autowired
    private lateinit var taxInvoiceProfiles: BuyerGroupTaxInvoiceProfileUseCase

    @Autowired
    private lateinit var businessRegistrationVerificationJob: BusinessRegistrationVerificationJob

    @Autowired
    private lateinit var operationAccounts: OperationAccountUseCase

    @Autowired
    private lateinit var payments: TransactionalPaymentUseCase

    @Autowired
    private lateinit var shipments: TransactionalShipmentUseCase

    @Autowired
    private lateinit var cancellations: TransactionalOrderCancellationUseCase

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
            BuyerGroupTaxInvoiceProfileCommand(
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
                "SELECT default_tax_invoice_requested FROM account WHERE public_id = ?",
                Boolean::class.java,
                accountId.toBytes(),
            ),
        ).isFalse()

        val invoiceOrder = orders.create(accountId, addressId, true, true)
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
        jdbc.update(
            "UPDATE account SET default_tax_invoice_requested = TRUE WHERE public_id = ?",
            standardAccountId.toBytes(),
        )
        val standardAddressId = createAddress(standardAccountId)
        createCartWithItem(standardAccountId, skuId)
        val standardOrder = orders.create(standardAccountId, standardAddressId, false, false)
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
        val order = orders.create(accountId, addressId, false, false)
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
        val cancelledOrder = orders.create(cancelledAccountId, cancelledAddressId, false, false)
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

    @Test
    fun `group invitations and join requests require recipient acceptance and representative approval`() {
        val representativeId = createAccount()
        val invitedId = createAccount()
        val requestedId = createAccount()
        val businessGroupId = createBusinessGroup(representativeId)
        buyerGroups.ensureForAccount(invitedId)
        buyerGroups.ensureForAccount(requestedId)

        groupMembership.invite(representativeId, accountPhone(invitedId))
        assertThat(groupMembership.invitations(invitedId)).hasSize(1)
        groupMembership.respondInvitation(invitedId, groupMembership.invitations(invitedId).single().id, true)
        assertThat(groupMembership.current(invitedId)?.id).isEqualTo(businessGroupId)
        assertThat(jdbc.queryForObject("SELECT status FROM buyer_group WHERE public_id = ?", String::class.java, personalGroupId(invitedId).toBytes()))
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
    fun `only the configured representative can invite members and review join requests`() {
        val formerRepresentative = createAccount()
        val memberId = createAccount()
        val applicantId = createAccount()
        val businessGroupId = createBusinessGroup(formerRepresentative)
        buyerGroups.ensureForAccount(memberId)
        buyerGroups.assignAccountToBusinessGroup(memberId, businessGroupId)
        buyerGroups.ensureForAccount(applicantId)

        operationAccounts.setBuyerGroupRepresentative(businessGroupId, memberId)
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
        buyerGroups.ensureForAccount(accountId)
        return customerAccounts.createAddress(
            accountId,
            SharedAddressCommand("Recipient", "01012345678", "12345", "Seoul address", "Details", false),
        ).id
    }

    private fun createBusinessGroup(accountId: UUID, verified: Boolean = true): UUID {
        val accountInternalId = jdbc.queryForObject("SELECT id FROM account WHERE public_id = ?", Long::class.java, accountId.toBytes())!!
        jdbc.update(
            "INSERT INTO business_profile (account_id, business_name, business_phone, status) VALUES (?, 'Group test business', '010-9000-0000', 'ACTIVE')",
            accountInternalId,
        )
        val group = buyerGroups.ensureForAccount(accountId)
        if (verified) {
            jdbc.update(
                "UPDATE buyer_group_business_profile SET business_registration_number = '987-65-43210', business_registration_verification_status = 'ACTIVE', business_registration_verified_at = CURRENT_TIMESTAMP(3), business_registration_confirmed_at = CURRENT_TIMESTAMP(3) WHERE buyer_group_id = ?",
                group.id,
            )
        } else {
            jdbc.update(
                "UPDATE buyer_group_business_profile SET business_registration_number = '987-65-43210' WHERE buyer_group_id = ?",
                group.id,
            )
        }
        entityManager.clear()
        return requireNotNull(group.publicId)
    }

    @Test
    fun `buyer group tax invoice profile is shared and order snapshot becomes issuable on shipment start`() {
        val ownerId = createAccount()
        val groupId = createBusinessGroup(ownerId)
        val command = BuyerGroupTaxInvoiceProfileCommand(
            "987-65-43210", "Group test business", "Buyer", "12345", "Buyer address", "Suite 2", "Retail", "Hardware", "buyer@example.com",
        )
        val saved = taxInvoiceProfiles.updateForAccount(ownerId, command)
        assertThat(saved.complete).isTrue()
        assertThat(taxInvoiceProfiles.forGroup(groupId).email).isEqualTo("buyer@example.com")

        val addressId = createAddress(ownerId)
        val categoryId = createCategory()
        val productId = createProduct(categoryId)
        val skuId = createSku(productId)
        createStock(skuId)
        createCartWithItem(ownerId, skuId)
        val order = orders.create(ownerId, addressId, true, false)
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

        jdbc.update("UPDATE buyer_group_business_profile SET business_name = 'Changed later' WHERE buyer_group_id = (SELECT id FROM buyer_group WHERE public_id = ?)", groupId.toBytes())
        assertThat(orders.detail(ownerId, order.id).taxInvoiceSnapshot?.buyer?.businessName).isEqualTo("Group test business")

        val memberId = createAccount()
        buyerGroups.ensureForAccount(memberId)
        buyerGroups.assignAccountToBusinessGroup(memberId, groupId)
        assertThatThrownBy { taxInvoiceProfiles.updateForAccount(memberId, command) }
            .isInstanceOf(com.buyeong.umji.api.exception.ForbiddenOperationException::class.java)
    }

    @Test
    fun `pre-registered group waits for background verification then representative confirmation`() {
        val ownerId = createAccount()
        createBusinessGroup(ownerId, verified = false)
        val command = BuyerGroupTaxInvoiceProfileCommand(
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
        val personalGroup = groupMembership.register(personalAccountId, BuyerGroupRegistrationCommand("INDIVIDUAL", null))
        assertThat(personalGroup.type).isEqualTo("INDIVIDUAL")
        assertThat(groupMembership.current(personalAccountId)?.id).isEqualTo(personalGroup.id)
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM buyer_group_business_profile WHERE buyer_group_id = (SELECT id FROM buyer_group WHERE public_id = ?)",
            Int::class.java,
            personalGroup.id.toBytes(),
        )).isZero()

        val businessAccountId = createAccount()
        Mockito.doNothing().`when`(businessRegistrationStatus).ensureNotClosed("1234567890")
        val businessGroup = groupMembership.register(
            businessAccountId,
            BuyerGroupRegistrationCommand(
                "BUSINESS",
                BusinessGroupRegistration(
                    "1234567890", "Buyer Business", "Buyer Owner", "12345", "Registered Place", "Building 1", "Retail", "Hardware", "billing@example.com", true,
                ),
            ),
        )
        assertThat(businessGroup.type).isEqualTo("BUSINESS")
        Mockito.verify(businessRegistrationStatus).ensureNotClosed("1234567890")
        assertThat(jdbc.queryForObject(
            "SELECT business_registration_verified_at IS NOT NULL FROM buyer_group_business_profile WHERE buyer_group_id = (SELECT id FROM buyer_group WHERE public_id = ?)",
            Boolean::class.java,
            businessGroup.id.toBytes(),
        )).isTrue()
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM buyer_group_address WHERE buyer_group_id = (SELECT id FROM buyer_group WHERE public_id = ?)",
            Int::class.java,
            businessGroup.id.toBytes(),
        )).isZero()
        assertThatThrownBy { groupMembership.register(businessAccountId, BuyerGroupRegistrationCommand("INDIVIDUAL", null)) }
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
                BuyerGroupRegistrationCommand(
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
            "SELECT buyer_group.public_id FROM buyer_group JOIN buyer_group_member ON buyer_group_member.buyer_group_id = buyer_group.id WHERE buyer_group_member.account_id = (SELECT id FROM account WHERE public_id = ?) AND buyer_group.group_type = 'INDIVIDUAL'",
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
