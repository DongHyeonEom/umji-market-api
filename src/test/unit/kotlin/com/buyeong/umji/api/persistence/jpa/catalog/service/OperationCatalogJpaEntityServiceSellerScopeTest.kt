package com.buyeong.umji.api.persistence.jpa.catalog.service

import com.buyeong.umji.api.domain.operation.catalog.model.ProductCommand
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.account.repository.OrganizationRepository
import com.buyeong.umji.api.persistence.jpa.catalog.entity.BrandEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.CategoryEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesChannelEntity
import com.buyeong.umji.api.persistence.jpa.catalog.repository.ChannelProductListingRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.SalesChannelRepository
import com.buyeong.umji.api.persistence.jpa.catalog.repository.SalesOfferRepository
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class OperationCatalogJpaEntityServiceSellerScopeTest : DescribeSpec({
    val catalog = mockk<CatalogJpaEntityService>()
    val channels = mockk<SalesChannelRepository>()
    val listings = mockk<ChannelProductListingRepository>()
    val offers = mockk<SalesOfferRepository>()
    val organizations = mockk<OrganizationRepository>()
    val service = OperationCatalogJpaEntityService(catalog, channels, listings, offers, organizations)

    it("다른 Organization 소유 브랜드를 판매자 상품에 연결하지 않는다") {
        val sellerOrganizationId = UUID.randomUUID()
        val seller = organization(1L, sellerOrganizationId)
        val foreignBrand = BrandEntity().apply {
            organization = organization(2L, UUID.randomUUID())
            name = "Foreign brand"
            displayStatus = "DISPLAYED"
        }
        val categoryId = UUID.randomUUID()
        val brandId = UUID.randomUUID()
        val category = CategoryEntity().apply {
            salesChannel = SalesChannelEntity().apply {
                code = "WHOLESALE"
                name = "Wholesale"
            }
            name = "Category"
            path = "Category"
            displayStatus = "DISPLAYED"
        }
        every { organizations.findByPublicId(sellerOrganizationId) } returns seller
        every { catalog.category(categoryId) } returns category
        every { catalog.brand(brandId) } returns foreignBrand

        shouldThrow<IllegalArgumentException> {
            service.createSellerProduct(
                sellerOrganizationId,
                ProductCommand(categoryId, brandId, "Product", null, "DISPLAYED", "ON_SALE", 0),
            )
        }

        verify(exactly = 0) { catalog.save(any<ProductEntity>()) }
    }

    it("다른 Organization 소유 상품은 수정 대상으로 반환하지 않는다") {
        val sellerOrganizationId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        every { organizations.findByPublicId(sellerOrganizationId) } returns organization(1L, sellerOrganizationId)
        every { catalog.sellerProduct(productId, sellerOrganizationId) } returns null

        service.updateSellerProduct(
            sellerOrganizationId,
            productId,
            ProductCommand(UUID.randomUUID(), null, "Changed", null, "DISPLAYED", "ON_SALE", 0),
        ) shouldBe null

        verify(exactly = 1) { catalog.sellerProduct(productId, sellerOrganizationId) }
    }
}) {
    companion object {
        private fun organization(id: Long, publicId: UUID) = OrganizationEntity().apply {
            this.id = id
            this.publicId = publicId
            organizationType = "BUSINESS"
            displayName = "Seller $id"
            status = "ACTIVE"
        }
    }
}