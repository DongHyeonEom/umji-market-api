package com.buyeong.umji.api.persistence.jpa.account.service

import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationBusinessProfileEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationBusinessProfileRepository
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.util.UUID

class OrganizationTaxInvoiceJpaEntityServiceSellerReadinessTest : DescribeSpec({
    fun fixture(
        organizationStatus: String = "ACTIVE",
        type: String = "BUSINESS",
        sellerCapability: Boolean = true,
        profile: OrganizationBusinessProfileEntity? = completeProfile(),
    ): Fixture {
        val organizationId = UUID.randomUUID()
        val organization = OrganizationEntity().apply {
            id = 42L
            publicId = organizationId
            organizationType = type
            displayName = "Seller"
            status = organizationStatus
        }
        val organizations = mockk<OrganizationJpaEntityService>()
        val groups = mockk<OrganizationRepository>()
        val profiles = mockk<OrganizationBusinessProfileRepository>()
        every { groups.findByPublicId(organizationId) } returns organization
        every { organizations.hasCapability(organizationId, "SELLER") } returns sellerCapability
        every { profiles.findByOrganization_Id(42L) } returns profile
        return Fixture(
            OrganizationTaxInvoiceJpaEntityService(organizations, groups, profiles),
            organizationId,
        )
    }

    it("활성 판매 Organization의 확인 완료 프로필은 판매 준비 완료로 본다") {
        val context = fixture()

        context.service.isSellerBusinessProfileReady(context.organizationId) shouldBe true
    }

    it("프로필 미등록, 미확인, 폐업, 미완성 프로필은 판매 준비가 되지 않은 것으로 본다") {
        val notReadyProfiles = listOf(
            null,
            completeProfile().apply { businessRegistrationVerificationStatus = "PENDING" },
            completeProfile().apply { businessRegistrationVerificationStatus = "CLOSED" },
            completeProfile().apply { businessRegistrationVerificationStatus = "TEMPORARILY_CLOSED" },
            completeProfile().apply { businessRegistrationConfirmedAt = null },
            completeProfile().apply { businessItem = null },
        )

        notReadyProfiles.forEach { profile ->
            val context = fixture(profile = profile)
            context.service.isSellerBusinessProfileReady(context.organizationId) shouldBe false
        }
    }

    it("비활성·개인·SELLER capability가 없는 Organization은 판매 준비가 되지 않는다") {
        val contexts = listOf(
            fixture(organizationStatus = "INACTIVE"),
            fixture(type = "INDIVIDUAL"),
            fixture(sellerCapability = false),
        )

        contexts.forEach { context ->
            context.service.isSellerBusinessProfileReady(context.organizationId) shouldBe false
        }
    }
}) {
    companion object {
        private data class Fixture(
            val service: OrganizationTaxInvoiceJpaEntityService,
            val organizationId: UUID,
        )

        private fun completeProfile() = OrganizationBusinessProfileEntity().apply {
            businessRegistrationNumber = "1234567890"
            businessName = "Seller Co"
            representativeName = "Representative"
            postalCode = "12345"
            address1 = "Business address"
            businessIndustry = "Wholesale"
            businessItem = "Goods"
            businessRegistrationVerificationStatus = "ACTIVE"
            businessRegistrationConfirmedAt = Instant.parse("2026-10-01T00:00:00Z")
            status = "COMPLETED"
        }
    }
}