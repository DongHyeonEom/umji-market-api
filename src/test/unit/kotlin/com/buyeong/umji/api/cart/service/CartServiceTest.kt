package com.buyeong.umji.api.cart.service

import com.buyeong.umji.api.persistence.jpa.account.AccountEntity
import com.buyeong.umji.api.persistence.jpa.catalog.ProductEntity
import com.buyeong.umji.api.persistence.jpa.catalog.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import com.buyeong.umji.api.cart.model.AddCartItemRequest
import com.buyeong.umji.api.persistence.jpa.cart.CartEntity
import com.buyeong.umji.api.persistence.jpa.cart.CartItemEntity
import com.buyeong.umji.api.persistence.jpa.cart.CartJpaEntityService
import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.UUID

class CartServiceTest : DescribeSpec({
    val carts = mockk<CartJpaEntityService>()
    val skus = mockk<CatalogJpaEntityService>()
    val service = CartService(carts, skus)
    val account = mockk<AccountEntity>()
    val sku = mockk<ProductSkuEntity>()
    val product = mockk<ProductEntity>()
    val skuId = UUID.randomUUID()

    beforeTest {
        every { account.id } returns 1L
        every { sku.id } returns 2L
        every { sku.publicId } returns skuId
        every { sku.skuCode } returns "SKU-001"
        every { sku.name } returns "기본 규격"
        every { sku.salePrice } returns 12000L
        every { sku.salesStatus } returns "ON_SALE"
        every { sku.product } returns product
        every { product.name } returns "테스트 상품"
        every { skus.sku(skuId) } returns sku
    }

    describe("장바구니 SKU 추가") {
        it("동일 SKU는 수량을 합산한다") {
            val cart = CartEntity().apply { this.account = account }
            val existing = CartItemEntity().apply {
                this.sku = sku
                quantity = 2
            }
            setPublicId(existing, UUID.randomUUID())
            cart.add(existing)
            every { carts.findLocked(1L) } returns cart
            every { carts.saveAndFlush(cart) } answers { cart }

            val response = service.add(account, AddCartItemRequest(skuId, 3))

            response.items.size shouldBe 1
            response.items.single().quantity shouldBe 5
        }
    }
}) {
    companion object {
        private fun setPublicId(entity: DomainPublicEntity, id: UUID) {
            DomainPublicEntity::class.java.getDeclaredField("publicId").apply { isAccessible = true }.set(entity, id)
        }
    }
}
