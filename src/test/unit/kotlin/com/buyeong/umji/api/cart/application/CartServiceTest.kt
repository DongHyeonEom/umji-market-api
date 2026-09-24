package com.buyeong.umji.api.cart.application

import com.buyeong.umji.api.cart.application.model.CartItemState
import com.buyeong.umji.api.cart.application.model.CartState
import com.buyeong.umji.api.cart.application.model.SellableSku
import com.buyeong.umji.api.cart.application.port.out.CartStorePort
import com.buyeong.umji.api.cart.application.port.out.SellableSkuQueryPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.util.UUID

class CartServiceTest : DescribeSpec({
    val carts = mockk<CartStorePort>()
    val skus = mockk<SellableSkuQueryPort>()
    val service = CartService(carts, skus)
    val accountId = UUID.randomUUID()
    val skuId = UUID.randomUUID()
    val itemId = UUID.randomUUID()
    val sku = SellableSku(skuId, "SKU-001", "테스트 상품", "기본 규격", 12000, "ON_SALE")

    describe("장바구니 SKU 추가") {
        it("동일 SKU는 수량을 합산한다") {
            every { skus.find(skuId) } returns sku
            every { carts.find(accountId) } returns CartState(accountId, listOf(CartItemState(itemId, sku, 2)))
            every { carts.save(any()) } answers { firstArg() }

            val result = service.add(accountId, com.buyeong.umji.api.cart.application.model.AddCartItemCommand(skuId, 3))

            result.items.size shouldBe 1
            result.items.single().quantity shouldBe 5
        }

        it("판매 중이 아닌 SKU는 추가하지 않는다") {
            every { skus.find(skuId) } returns sku.copy(salesStatus = "STOPPED")
            shouldThrow<IllegalArgumentException> {
                service.add(accountId, com.buyeong.umji.api.cart.application.model.AddCartItemCommand(skuId, 1))
            }
        }
    }
})