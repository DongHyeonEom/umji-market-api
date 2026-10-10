package com.buyeong.umji.api.domain.cart.service

import com.buyeong.umji.api.domain.cart.dto.AddCartItemCommandDto
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.cart.entity.CartEntity
import com.buyeong.umji.api.persistence.jpa.cart.service.CartJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesChannelEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesOfferEntity
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class CartServiceTest : DescribeSpec({
    val catalog = mockk<CatalogJpaEntityService>()
    val accounts = mockk<AccountJpaEntityService>()
    val carts = mockk<CartJpaEntityService>()
    val service = CartService(
        accounts,
        carts,
        catalog,
    )

    describe("장바구니 상품 추가") {
        it("SKU와 판매 오퍼 ID 중 하나를 지정해야 한다") {
            shouldThrow<IllegalArgumentException> {
                service.add(UUID.randomUUID(), AddCartItemCommandDto(null, null, quantity = 1))
            }
        }

        it("존재하지 않는 오퍼는 찾을 수 없음으로 처리한다") {
            val offerId = UUID.randomUUID()
            every { catalog.salesOffer(offerId) } returns null
            shouldThrow<ItemNotFoundException> {
                service.add(UUID.randomUUID(), AddCartItemCommandDto(null, offerId, "RETAIL", 1))
            }
        }

        it("장바구니 응답에 판매 오퍼의 Organization 소유 정보를 유지한다") {
            val accountId = UUID.randomUUID()
            val sellerOrganizationId = UUID.randomUUID()
            val offerId = UUID.randomUUID()
            val skuId = UUID.randomUUID()
            val account = AccountEntity().apply { id = 1L }
            val seller = OrganizationEntity().apply { publicId = sellerOrganizationId }
            val sku = ProductSkuEntity().apply {
                product = ProductEntity().apply { name = "Seller product" }
                skuCode = "SELLER-001"
                name = "Seller SKU"
                setPublicId(skuId)
            }
            val offer = SalesOfferEntity().apply {
                salesChannel = SalesChannelEntity().apply {
                    code = "WHOLESALE"
                    name = "Wholesale"
                }
                organization = seller
                productSku = sku
                salePrice = 1250
                salesStatus = "ON_SALE"
                unitsPerSale = 4
                setPublicId(offerId)
            }
            val cart = CartEntity().apply { this.account = account }
            every { accounts.findByPublicId(accountId) } returns account
            every { carts.findLocked(1L) } returns cart
            every { catalog.salesOffer(offerId) } returns offer
            every { catalog.sku(skuId) } returns sku
            every { carts.saveAndFlush(any()) } answers {
                firstArg<CartEntity>().also { saved -> saved.items.forEach { it.setPublicId(UUID.randomUUID()) } }
            }

            val result = service.add(accountId, AddCartItemCommandDto(null, offerId, "WHOLESALE", 2))

            result.items.single().salesOfferId shouldBe offerId
            result.items.single().sellerOrganizationId shouldBe sellerOrganizationId
            result.items.single().quantity shouldBe 2
            cart.items.single().sku shouldBe sku
            cart.items.single().salesOffer shouldBe offer
            verify(exactly = 1) { carts.saveAndFlush(cart) }
        }
    }
})

private fun Any.setPublicId(value: UUID) {
    val field = com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity::class.java.getDeclaredField("publicId")
    field.isAccessible = true
    field.set(this, value)
}