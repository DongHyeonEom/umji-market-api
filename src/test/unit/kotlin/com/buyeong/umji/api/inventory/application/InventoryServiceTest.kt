package com.buyeong.umji.api.inventory.application

import com.buyeong.umji.api.inventory.application.model.SkuReference
import com.buyeong.umji.api.inventory.application.model.StockState
import com.buyeong.umji.api.inventory.application.port.out.InventoryStorePort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class InventoryServiceTest : DescribeSpec({
    val store = mockk<InventoryStorePort>(relaxed = true)
    val service = InventoryService(store)
    val skuId = UUID.randomUUID()
    val sku = SkuReference(skuId, "SKU-001")

    describe("운영 재고 조정") {
        it("실재고를 조정하고 변동 이력을 남긴다") {
            every { store.lockStock(skuId) } returns StockState(sku, 10, 2, 0)
            every { store.saveStock(any()) } answers { firstArg() }

            val result = service.adjust(skuId, 5, "INITIAL_RECEIPT", "입고", null)

            result.onHand shouldBe 15
            result.available shouldBe 13
            verify(exactly = 1) { store.saveMovement(sku, "ADJUSTMENT", 5, "INITIAL_RECEIPT", null, "입고") }
        }

        it("예약 재고보다 낮게 실재고를 조정하지 못한다") {
            every { store.lockStock(skuId) } returns StockState(sku, 10, 8, 0)
            shouldThrow<IllegalArgumentException> { service.adjust(skuId, -3, "CORRECTION", null, null) }
        }
    }

    describe("재고 예약") {
        it("가용 재고보다 많은 수량은 예약하지 못한다") {
            val key = UUID.randomUUID()
            every { store.reservation(key) } returns null
            every { store.lockStock(skuId) } returns StockState(sku, 5, 2, 0)
            shouldThrow<IllegalArgumentException> { service.reserve(skuId, 4, key, null) }
        }
    }
})