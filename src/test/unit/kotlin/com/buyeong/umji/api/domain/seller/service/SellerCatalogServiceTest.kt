package com.buyeong.umji.api.domain.seller.service

import com.buyeong.umji.api.domain.account.model.OrganizationSummary
import com.buyeong.umji.api.domain.account.service.OrganizationMembershipService
import com.buyeong.umji.api.domain.account.service.OrganizationTaxInvoiceProfileService
import com.buyeong.umji.api.exception.ClientBadRequestException
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.domain.inventory.service.InventoryService
import com.buyeong.umji.api.domain.operation.catalog.model.BrandView
import com.buyeong.umji.api.domain.operation.catalog.model.ProductPageView
import com.buyeong.umji.api.domain.operation.catalog.model.SalesOfferView
import com.buyeong.umji.api.domain.operation.catalog.service.OperationCatalogService
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class SellerCatalogServiceTest : DescribeSpec({
    val organizations = mockk<OrganizationMembershipService>()
    val catalog = mockk<OperationCatalogService>()
    val inventory = mockk<InventoryService>()
    val commonCatalog = mockk<CatalogJpaEntityService>()
    val businessProfiles = mockk<OrganizationTaxInvoiceProfileService>()
    val service = SellerCatalogService(organizations, catalog, inventory, commonCatalog, businessProfiles)
    val accountId = UUID.randomUUID()
    val organizationId = UUID.randomUUID()

    beforeTest {
        clearMocks(organizations, catalog, inventory, commonCatalog, businessProfiles)
        every { organizations.current(accountId) } returns OrganizationSummary(
            organizationId,
            "BUSINESS",
            "Seller",
            true,
            setOf("SELLER"),
        )
    }

    describe("판매자 카탈로그 조회") {
        it("브랜드와 상품 조회를 현재 판매 Organization 범위로 제한한다") {
            val brands = listOf(BrandView(UUID.randomUUID(), "Brand", "DISPLAYED"))
            val products = mockk<ProductPageView>()
            every { catalog.sellerBrands(organizationId, 2, 25) } returns brands
            every { catalog.sellerProducts(organizationId, 3, 10) } returns products

            service.brands(accountId, 2, 25) shouldBe brands
            service.products(accountId, 3, 10) shouldBe products

            verify(exactly = 1) { catalog.sellerBrands(organizationId, 2, 25) }
            verify(exactly = 1) { catalog.sellerProducts(organizationId, 3, 10) }
        }

        it("SELLER capability가 없는 Organization은 카탈로그 조회를 할 수 없다") {
            every { organizations.current(accountId) } returns OrganizationSummary(
                organizationId,
                "BUSINESS",
                "Buyer",
                true,
                setOf("BUYER"),
            )

            shouldThrow<IllegalArgumentException> { service.brands(accountId, 0, 20) }

            verify(exactly = 0) { catalog.sellerBrands(any(), any(), any()) }
        }

        it("활성 Organization이 없는 계정은 카탈로그 조회를 할 수 없다") {
            every { organizations.current(accountId) } returns null

            shouldThrow<ItemNotFoundException> { service.products(accountId, 0, 20) }

            verify(exactly = 0) { catalog.sellerProducts(any(), any(), any()) }
        }
    }

    describe("판매 오퍼 변경") {
        it("확인 완료된 사업자 프로필이 없으면 판매 시작을 거부한다") {
            val skuId = UUID.randomUUID()
            every { businessProfiles.isSellerBusinessProfileReady(organizationId) } returns false

            shouldThrow<ClientBadRequestException> {
                service.updateOffer(accountId, "WHOLESALE", skuId, 1000, null, "ON_SALE", 1)
            }

            verify(exactly = 0) { commonCatalog.sku(any()) }
            verify(exactly = 0) { catalog.updateSellerSalesOffer(any(), any()) }
        }

        it("확인 완료 프로필과 소유 SKU가 있으면 판매 오퍼를 변경한다") {
            val skuId = UUID.randomUUID()
            val seller = OrganizationEntity().apply { publicId = organizationId }
            val product = ProductEntity().apply { organization = seller }
            val sku = ProductSkuEntity().apply { this.product = product }
            every { businessProfiles.isSellerBusinessProfileReady(organizationId) } returns true
            every { commonCatalog.sku(skuId) } returns sku
            every { catalog.updateSellerSalesOffer(any(), any()) } returns
                SalesOfferView(UUID.randomUUID(), "WHOLESALE", skuId, 1000, null, "ON_SALE", 1, organizationId)

            service.updateOffer(accountId, "WHOLESALE", skuId, 1000, null, "ON_SALE", 1).salesStatus shouldBe "ON_SALE"

            verify(exactly = 1) { catalog.updateSellerSalesOffer(organizationId, match { it.skuId == skuId && it.salesStatus == "ON_SALE" }) }
        }

        it("다른 Organization SKU의 판매 오퍼 변경을 거부한다") {
            val skuId = UUID.randomUUID()
            val foreign = OrganizationEntity().apply { publicId = UUID.randomUUID() }
            val product = ProductEntity().apply { organization = foreign }
            every { businessProfiles.isSellerBusinessProfileReady(organizationId) } returns true
            every { commonCatalog.sku(skuId) } returns ProductSkuEntity().apply { this.product = product }

            shouldThrow<ItemNotFoundException> {
                service.updateOffer(accountId, "WHOLESALE", skuId, 1000, null, "ON_SALE", 1)
            }

            verify(exactly = 0) { catalog.updateSellerSalesOffer(any(), any()) }
        }
    }

    describe("판매자 재고 조회와 변경") {
        it("다른 Organization SKU의 재고 조회·조정·이력 조회를 모두 거부한다") {
            val skuId = UUID.randomUUID()
            val foreign = OrganizationEntity().apply { publicId = UUID.randomUUID() }
            val product = ProductEntity().apply { organization = foreign }
            every { commonCatalog.sku(skuId) } returns ProductSkuEntity().apply { this.product = product }

            shouldThrow<ItemNotFoundException> { service.stock(accountId, skuId) }
            shouldThrow<ItemNotFoundException> { service.adjustStock(accountId, skuId, 1, "RECEIPT", null, null) }
            shouldThrow<ItemNotFoundException> { service.movements(accountId, skuId, 0, 20) }

            verify(exactly = 0) { inventory.stock(any(), any()) }
            verify(exactly = 0) { inventory.adjust(any(), any(), any(), any(), any(), any()) }
            verify(exactly = 0) { inventory.movements(any(), any(), any(), any()) }
        }
    }
})