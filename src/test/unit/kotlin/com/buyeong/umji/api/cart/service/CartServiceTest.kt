package com.buyeong.umji.api.cart.service

import com.buyeong.umji.api.cart.model.AddCartItemCommand
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.cart.service.CartJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import java.util.UUID

class CartServiceTest : DescribeSpec({
    val catalog = mockk<CatalogJpaEntityService>()
    val service = CartService(
        mockk<AccountJpaEntityService>(),
        mockk<CartJpaEntityService>(),
        catalog,
    )

    describe("장바구니 상품 추가") {
        it("SKU와 판매 오퍼 ID 중 하나를 지정해야 한다") {
            shouldThrow<IllegalArgumentException> {
                service.add(UUID.randomUUID(), AddCartItemCommand(null, null, quantity = 1))
            }
        }

        it("존재하지 않는 오퍼는 찾을 수 없음으로 처리한다") {
            val offerId = UUID.randomUUID()
            every { catalog.salesOffer(offerId) } returns null
            shouldThrow<ItemNotFoundException> {
                service.add(UUID.randomUUID(), AddCartItemCommand(null, offerId, "RETAIL", 1))
            }
        }
    }
})
