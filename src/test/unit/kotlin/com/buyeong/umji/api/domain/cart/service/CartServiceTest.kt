package com.buyeong.umji.api.domain.cart.service

import com.buyeong.umji.api.domain.cart.dto.AddCartItemCommandDto
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationJpaEntityService
import com.buyeong.umji.api.persistence.jpa.cart.entity.CartEntity
import com.buyeong.umji.api.persistence.jpa.cart.entity.CartItemEntity
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
    val organizations = mockk<OrganizationJpaEntityService>()
    val service = CartService(
        accounts,
        carts,
        catalog,
        organizations,
    )

    describe("장바구니 상품 추가") {
        it("SKU와 판매 오퍼 ID 중 하나를 지정해야 한다") {
            shouldThrow<IllegalArgumentException> {
                service.add(UUID.randomUUID(), AddCartItemCommandDto(null, null, channelCode = null, quantity = 1))
            }
        }

        it("존재하지 않는 오퍼는 찾을 수 없음으로 처리한다") {
            val offerId = UUID.randomUUID()
            val accountId = UUID.randomUUID()
            every { organizations.buyerTypeForAccountPublicId(accountId) } returns "WHOLESALE_BUSINESS"
            every { catalog.salesOffer(offerId) } returns null
            shouldThrow<ItemNotFoundException> {
                service.add(accountId, AddCartItemCommandDto(null, offerId, "RETAIL", 1))
            }
        }

        it("장바구니 응답에 판매 오퍼의 Organization 소유 정보를 유지한다") {
            val accountId = UUID.randomUUID()
            val sellerOrganizationId = UUID.randomUUID()
            val offerId = UUID.randomUUID()
            val skuId = UUID.randomUUID()
            val account = AccountEntity().apply { id = 1L }
            every { organizations.buyerTypeForAccountPublicId(accountId) } returns "WHOLESALE_BUSINESS"
            val seller = OrganizationEntity().apply { publicId = sellerOrganizationId }
            val sku = ProductSkuEntity().apply {
                product = ProductEntity().apply {
                    name = "Seller product"
                    setPublicId(UUID.randomUUID())
                }
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
            every { catalog.salesOffer("WHOLESALE", skuId, sellerOrganizationId) } returns offer
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

    describe("상품별 수량 도매 가격") {
        it("동일 상품의 서로 다른 SKU 수량을 합산하고 상품별 기준 미달 상품은 소매가를 유지한다") {
            val accountId = UUID.randomUUID()
            val account = AccountEntity().apply { id = 10L }
            val cart = CartEntity().apply { this.account = account }
            val sharedProduct = product("합산 상품", minimum = 3)
            val otherProduct = product("별도 상품", minimum = 2)
            cart.add(cartItem(sharedProduct, "SKU-A", quantity = 2, retail = 1000, wholesale = 700))
            cart.add(cartItem(sharedProduct, "SKU-B", quantity = 1, retail = 1200, wholesale = 800))
            cart.add(cartItem(otherProduct, "SKU-C", quantity = 1, retail = 2000, wholesale = 1500))
            cart.items.forEach { it.setPublicId(UUID.randomUUID()) }
            every { accounts.findByPublicId(accountId) } returns account
            every { carts.findWithItems(10L) } returns cart
            every { organizations.buyerTypeForAccountPublicId(accountId) } returns "WHOLESALE_BUSINESS"

            val result = service.cart(accountId)

            result.items.map { it.pricingTier } shouldBe listOf("WHOLESALE", "WHOLESALE", "RETAIL")
            result.items.map { it.unitPrice } shouldBe listOf(700L, 800L, 2000L)
        }

        it("기준 수량 1·3·5에서 기준 직전은 소매가, 기준 도달은 도매가를 적용한다") {
            listOf(1, 3, 5).forEach { minimum ->
                val accountId = UUID.randomUUID()
                val account = AccountEntity().apply { id = minimum.toLong() }
                every { accounts.findByPublicId(accountId) } returns account
                every { organizations.buyerTypeForAccountPublicId(accountId) } returns "WHOLESALE_BUSINESS"
                val sku = cartItem(product("기준 $minimum", minimum), "SKU-$minimum", minimum, 1000, 600)
                val cartBefore = CartEntity().apply {
                    this.account = account
                    add(sku)
                }
                sku.setPublicId(UUID.randomUUID())
                every { carts.findWithItems(minimum.toLong()) } returns cartBefore
                if (minimum > 1) {
                    sku.quantity = minimum - 1
                    service.cart(accountId).items.single().unitPrice shouldBe 1000L
                }
                sku.quantity = minimum
                service.cart(accountId).items.single().unitPrice shouldBe 600L
            }
        }

        it("일반 업체와 일반인은 기준을 넘겨도 소매가를 적용한다") {
            listOf("GENERAL_BUSINESS", "INDIVIDUAL").forEachIndexed { index, buyerType ->
                val accountId = UUID.randomUUID()
                val account = AccountEntity().apply { id = (30 + index).toLong() }
                val item = cartItem(product("일반 구매", 1), "SKU-$buyerType", 1, 1000, 600)
                item.setPublicId(UUID.randomUUID())
                val cart = CartEntity().apply {
                    this.account = account
                    add(item)
                }
                every { accounts.findByPublicId(accountId) } returns account
                every { carts.findWithItems((30 + index).toLong()) } returns cart
                every { organizations.buyerTypeForAccountPublicId(accountId) } returns buyerType

                val result = service.cart(accountId).items.single()

                result.pricingTier shouldBe "RETAIL"
                result.unitPrice shouldBe 1000L
            }
        }
    }
})

private fun product(name: String, minimum: Int) = ProductEntity().apply {
    this.name = name
    wholesaleMinimumQuantity = minimum
}

private fun cartItem(product: ProductEntity, skuCode: String, quantity: Int, retail: Long, wholesale: Long) =
    CartItemEntity().apply {
        val sku = ProductSkuEntity().apply {
            this.product = product
            this.skuCode = skuCode
            name = skuCode
            salesStatus = "ON_SALE"
            setPublicId(UUID.randomUUID())
        }
        sku.product.setPublicIdIfMissing()
        val offer = SalesOfferEntity().apply {
            salesChannel = SalesChannelEntity().apply {
                code = "WHOLESALE"
                name = "통합 카탈로그"
            }
            productSku = sku
            retailSalePrice = retail
            wholesaleSalePrice = wholesale
            salePrice = retail
            salesStatus = "ON_SALE"
            setPublicId(UUID.randomUUID())
        }
        this.sku = sku
        salesOffer = offer
        this.quantity = quantity
    }

private fun ProductEntity.setPublicIdIfMissing() {
    setPublicId(UUID.randomUUID())
}

private fun Any.setPublicId(value: UUID) {
    val field = com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity::class.java.getDeclaredField("publicId")
    field.isAccessible = true
    field.set(this, value)
}