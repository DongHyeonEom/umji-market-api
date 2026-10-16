package com.buyeong.umji.api.persistence.jpa.account.service

import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationBusinessProfileRepository
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationCapabilityRepository
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationMemberRepository
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class OrganizationJpaEntityServiceCommercePolicyTest : DescribeSpec({
    val accounts = mockk<AccountJpaEntityService>()
    val organizations = mockk<OrganizationRepository>()
    val members = mockk<OrganizationMemberRepository>()
    val profiles = mockk<OrganizationBusinessProfileRepository>()
    val capabilities = mockk<OrganizationCapabilityRepository>()
    val service = OrganizationJpaEntityService(accounts, organizations, members, profiles, capabilities)

    it("선결제 출고 제한을 지정한 구매 Organization에만 저장한다") {
        val organizationId = UUID.randomUUID()
        val organization = OrganizationEntity().apply {
            id = 21L
            publicId = organizationId
            organizationType = "BUSINESS"
            buyerType = "GENERAL_BUSINESS"
            displayName = "선결제 업체"
            status = "ACTIVE"
        }
        val otherOrganization = OrganizationEntity().apply {
            id = 22L
            organizationType = "BUSINESS"
            buyerType = "GENERAL_BUSINESS"
            displayName = "기본 업체"
            status = "ACTIVE"
        }
        every { organizations.findByPublicId(organizationId) } returns organization
        every { organizations.findLockedById(21L) } returns organization
        every { capabilities.existsByOrganization_IdAndCapabilityCode(21L, "BUYER") } returns true
        every { organizations.saveAndFlush(organization) } returns organization

        val result = service.updateCommercePolicy(organizationId, "WHOLESALE_BUSINESS", true)

        result.paymentBeforeShipmentRequired shouldBe true
        result.buyerType shouldBe "WHOLESALE_BUSINESS"
        organization.paymentBeforeShipmentRequired shouldBe true
        otherOrganization.paymentBeforeShipmentRequired shouldBe false
        verify(exactly = 1) { organizations.saveAndFlush(organization) }
    }
})