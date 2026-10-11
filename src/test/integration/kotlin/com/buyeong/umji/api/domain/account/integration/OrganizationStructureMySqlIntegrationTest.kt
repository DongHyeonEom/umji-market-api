package com.buyeong.umji.api.domain.account.integration

import com.buyeong.umji.api.domain.account.dto.BusinessGroupRegistrationDto
import com.buyeong.umji.api.domain.account.dto.OrganizationRegistrationCommandDto
import com.buyeong.umji.api.domain.account.dto.SharedAddressCommandDto
import com.buyeong.umji.api.domain.account.integration.http.BusinessRegistrationStatusClient
import com.buyeong.umji.api.domain.account.service.CustomerAccountService
import com.buyeong.umji.api.domain.account.service.OrganizationMembershipService
import com.buyeong.umji.api.domain.account.service.OrganizationTaxInvoiceProfileService
import com.buyeong.umji.api.domain.operation.account.dto.NewAccountDto
import com.buyeong.umji.api.domain.operation.account.dto.OrganizationProfileDataDto
import com.buyeong.umji.api.domain.operation.account.service.OperationAccountService
import com.buyeong.umji.api.exception.ForbiddenOperationException
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationMembershipJpaEntityService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import java.nio.ByteBuffer
import java.util.UUID

@SpringBootTest
@ActiveProfiles("local")
@Transactional
@Rollback
class OrganizationStructureMySqlIntegrationTest {
    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var memberships: OrganizationMembershipService

    @Autowired
    private lateinit var membershipEntities: OrganizationMembershipJpaEntityService

    @Autowired
    private lateinit var organizations: OrganizationJpaEntityService

    @Autowired
    private lateinit var taxInvoiceProfiles: OrganizationTaxInvoiceProfileService

    @Autowired
    private lateinit var customerAccounts: CustomerAccountService

    @Autowired
    private lateinit var operationAccounts: OperationAccountService

    @MockitoBean
    private lateinit var businessRegistrationStatus: BusinessRegistrationStatusClient

    @Test
    fun `business buyer seller organizations share profiles and enforce representative changes`() {
        Mockito.doNothing().`when`(businessRegistrationStatus).ensureNotClosed(anyString())
        val buyerAccount = createAccount("buyer")
        val sellerAccount = createAccount("seller")

        val buyer = memberships.register(buyerAccount, businessRegistration("BUYER", "Buyer Co"))
        val seller = memberships.register(sellerAccount, businessRegistration("SELLER", "Seller Co"))
        Mockito.verify(businessRegistrationStatus, times(2)).ensureNotClosed("1234567890")
        jdbc.update(
            "INSERT INTO organization_capability (organization_id, capability_code) SELECT id, 'SELLER' FROM organization WHERE public_id = ?",
            buyer.id.bytes(),
        )

        assertThat(memberships.current(buyerAccount)?.capabilities).contains("BUYER", "SELLER")
        assertThat(seller.capabilities).containsExactly("SELLER")
        assertThat(memberships.current(buyerAccount)?.id).isEqualTo(buyer.id)
        assertThat(taxInvoiceProfiles.forAccount(buyerAccount).complete).isTrue()
        assertThat(taxInvoiceProfiles.isSellerBusinessProfileReady(seller.id)).isTrue()
        assertThat(
            jdbc.queryForObject(
                "SELECT business_registration_number FROM organization_business_profile p JOIN organization o ON o.id = p.organization_id WHERE o.public_id = ?",
                String::class.java,
                seller.id.bytes(),
            )
        ).isEqualTo("1234567890")

        val memberAccount = createAccount("buyer-member")
        memberships.invite(buyerAccount, phoneFor(memberAccount))
        val invitation = memberships.invitations(memberAccount).single()
        memberships.respondInvitation(memberAccount, invitation.id, accept = true)
        assertThat(memberships.current(memberAccount)?.id).isEqualTo(buyer.id)
        assertThat(memberships.current(memberAccount)?.representative).isFalse()
        assertThat(taxInvoiceProfiles.forAccount(memberAccount).businessName).isEqualTo("Buyer Co")
        val address = customerAccounts.createAddress(
            buyerAccount,
            SharedAddressCommandDto("Buyer Rep", "01011112222", "01234", "Shared Shipping Road", "1F", true),
        )
        assertThat(customerAccounts.addresses(memberAccount).single().id).isEqualTo(address.id)

        val sellerMember = createAccount("seller-member")
        memberships.invite(sellerAccount, phoneFor(sellerMember))
        val sellerInvitation = memberships.invitations(sellerMember).single()
        memberships.respondInvitation(sellerMember, sellerInvitation.id, accept = true)
        assertThat(memberships.current(sellerMember)?.id).isEqualTo(seller.id)
        assertThat(taxInvoiceProfiles.forAccount(sellerMember).businessName).isEqualTo("Seller Co")

        organizations.updateDefaultTaxInvoiceRequestedForAccount(buyerAccount, true)
        assertThat(organizations.defaultTaxInvoiceRequestedForAccount(memberAccount)).isTrue()
        assertThatThrownBy {
            organizations.updateDefaultTaxInvoiceRequestedForAccount(memberAccount, false)
        }.isInstanceOf(ForbiddenOperationException::class.java)

        val unassigned = createAccount("unassigned")
        organizations.assignAccountToOrganization(unassigned, seller.id)
        assertThat(memberships.current(unassigned)?.id).isEqualTo(seller.id)
        assertThat(membershipEntities.search(phoneFor(sellerAccount), "SELLER").map { it.id }).contains(seller.id)
        assertThat(membershipEntities.search(phoneFor(sellerAccount), "BUYER").map { it.id }).doesNotContain(seller.id)
    }

    @Test
    fun `operator provisions organization profile assigns account and changes representative`() {
        val representative = createAccount("operator-buyer")
        val buyer = memberships.createIndividualGroup(representative, "운영자 연결 대상")
        val phone = "01098765432"
        val provisioned = operationAccounts.create(
            NewAccountDto(
                name = "사전 등록 구성원",
                phone = phone,
                normalizedPhone = phone,
                email = null,
                profile = OrganizationProfileDataDto("사전 등록 판매자", "1234567890", "홍길동", null, "01234", "서울", null),
                organizationCapability = "SELLER",
            ),
        )
        val originalOrganization = requireNotNull(provisioned.organizationId)
        assertThat(provisioned.organizationCapabilities).containsExactly("SELLER")

        val profile = OrganizationProfileDataDto("수정 판매자", "1234567890", "김길동", null, "04567", "부산", null)
        operationAccounts.profile(provisioned.id, profile)
        assertThat(taxInvoiceProfiles.forGroup(originalOrganization).businessName).isEqualTo("수정 판매자")

        operationAccounts.assignOrganization(provisioned.id, buyer.id)
        assertThat(memberships.current(provisioned.id)?.id).isEqualTo(buyer.id)
        operationAccounts.setOrganizationRepresentative(buyer.id, provisioned.id)
        assertThat(memberships.current(provisioned.id)?.representative).isTrue()
        assertThat(memberships.current(representative)?.representative).isFalse()
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM organization_member WHERE account_id = (SELECT id FROM account WHERE public_id = ?) AND status = 'ACTIVE'",
                Int::class.java,
                provisioned.id.bytes(),
            )
        ).isEqualTo(1)
    }

    @Test
    fun `individual organization join approval moves membership and capability remains buyer scoped`() {
        val representative = createAccount("individual-representative")
        val applicant = createAccount("individual-applicant")
        val organization = memberships.createIndividualGroup(representative, "개인 구매 그룹")
        val applicantOrganization = memberships.createIndividualGroup(applicant, "가입 요청자 개인 그룹")
        val previousAddress = customerAccounts.createAddress(
            applicant,
            SharedAddressCommandDto("Applicant", "01011113333", "09999", "Previous Address", null, true),
        )
        val applicantAccountId = internalAccountId(applicant)
        val applicantOrganizationId = internalOrganizationId(applicantOrganization.id)
        val previousOrderNumber = "ORG-MOVE-${UUID.randomUUID().toString().take(8)}"
        jdbc.update(
            """INSERT INTO purchase_order (public_id, order_number, account_id, created_by_account_id, organization_id,
                status, subtotal_amount, total_amount, ordered_at)
                VALUES (?, ?, ?, ?, ?, 'PENDING_PAYMENT', 1000, 1000, CURRENT_TIMESTAMP(3))""",
            UUID.randomUUID().bytes(),
            previousOrderNumber,
            applicantAccountId,
            applicantAccountId,
            applicantOrganizationId,
        )
        memberships.requestToJoin(applicant, organization.id)
        val request = memberships.pendingJoinRequests(representative).single()
        memberships.respondJoinRequest(representative, request.id, approve = true)

        assertThat(memberships.current(applicant)?.id).isEqualTo(organization.id)
        assertThat(memberships.current(applicant)?.capabilities).containsExactly("BUYER")
        assertThat(
            jdbc.queryForObject("SELECT status FROM organization WHERE public_id = ?", String::class.java, applicantOrganization.id.bytes())
        ).isEqualTo("INACTIVE")
        assertThat(
            jdbc.queryForObject("SELECT organization_id FROM purchase_order WHERE order_number = ?", Long::class.java, previousOrderNumber)
        ).isEqualTo(applicantOrganizationId)
        assertThat(
            jdbc.queryForObject(
                "SELECT organization_id FROM organization_address WHERE public_id = ?",
                Long::class.java,
                previousAddress.id.bytes(),
            )
        ).isEqualTo(applicantOrganizationId)
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM organization_member m JOIN organization o ON o.id = m.organization_id WHERE o.public_id = ? AND m.status = 'ACTIVE'",
                Int::class.java,
                organization.id.bytes(),
            )
        ).isEqualTo(2)
        assertThat(taxInvoiceProfiles.forAccount(applicant).complete).isFalse()
    }

    private fun businessRegistration(capability: String, name: String) = OrganizationRegistrationCommandDto(
        type = "BUSINESS",
        capability = capability,
        business = BusinessGroupRegistrationDto(
            businessRegistrationNumber = "123-45-67890",
            businessName = name,
            representativeName = "홍길동",
            postalCode = "01234",
            address1 = "서울시 중구",
            address2 = null,
            businessIndustry = "도매업",
            businessItem = "잡화",
            email = null,
            confirmed = true,
        ),
    )

    private fun createAccount(prefix: String): UUID {
        val id = UUID.randomUUID()
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        val phone = "010${suffix.filter(Char::isDigit).padEnd(8, '0').take(8)}"
        jdbc.update(
            """INSERT INTO account (public_id, login_id, password_hash, name, phone, phone_normalized, status)
                VALUES (?, ?, 'test-hash', ?, ?, ?, 'ACTIVE')""",
            id.bytes(),
            "$prefix-$suffix",
            prefix,
            phone,
            phone,
        )
        return id
    }

    private fun phoneFor(accountId: UUID): String = jdbc.queryForObject(
        "SELECT phone_normalized FROM account WHERE public_id = ?",
        String::class.java,
        accountId.bytes(),
    )!!

    private fun internalAccountId(accountId: UUID): Long = jdbc.queryForObject(
        "SELECT id FROM account WHERE public_id = ?",
        Long::class.java,
        accountId.bytes(),
    )!!

    private fun internalOrganizationId(organizationId: UUID): Long = jdbc.queryForObject(
        "SELECT id FROM organization WHERE public_id = ?",
        Long::class.java,
        organizationId.bytes(),
    )!!

    private fun UUID.bytes(): ByteArray = ByteBuffer.allocate(16)
        .putLong(mostSignificantBits)
        .putLong(leastSignificantBits)
        .array()
}