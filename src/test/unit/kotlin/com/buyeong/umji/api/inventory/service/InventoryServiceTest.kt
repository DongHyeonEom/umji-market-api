package com.buyeong.umji.api.inventory.service

import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesOfferEntity
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import com.buyeong.umji.api.persistence.jpa.inventory.entity.InventoryStockEntity
import com.buyeong.umji.api.persistence.jpa.inventory.entity.StockReservationEntity
import com.buyeong.umji.api.persistence.jpa.inventory.service.InventoryJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class InventoryServiceTest : DescribeSpec({
    val catalog = mockk<CatalogJpaEntityService>()
    val inventory = mockk<InventoryJpaEntityService>()
    val service = InventoryService(catalog, inventory)
    val skuId = UUID.randomUUID()
    val skuEntity = mockk<ProductSkuEntity>()

    beforeTest {
        every { catalog.salesOffer("WHOLESALE", skuId) } returns mockk<SalesOfferEntity> { every { unitsPerSale } returns 12 }
        every { catalog.sku(skuId) } returns skuEntity
        every { skuEntity.id } returns 9L
        every { skuEntity.publicId } returns skuId
        every { skuEntity.skuCode } returns "SKU-001"
        every { inventory.saveMovement(any()) } answers { firstArg() }
        every { inventory.saveStock(any()) } answers { firstArg() }
        every { inventory.saveReservation(any()) } answers { firstArg() }
    }

    fun stock(onHand: Int, reserved: Int, safety: Int = 0) = InventoryStockEntity().apply {
        sku = skuEntity
        onHandQuantity = onHand
        reservedQuantity = reserved
        safetyStockQuantity = safety
    }

    describe("운영 재고 조정") {
        it("실재고를 조정하고 변동 이력을 남긴다") {
            val entity = stock(10, 2)
            every { inventory.lockedStock(skuEntity) } returns entity

            val result = service.adjust(skuId, 5, "INITIAL_RECEIPT", "입고", null)

            result.onHand shouldBe 15
            result.available shouldBe 13
            result.unitsPerSale shouldBe 12
            result.onHandBoxes shouldBe 1
            result.onHandRemainder shouldBe 3
            result.reservedBoxes shouldBe 0
            result.reservedRemainder shouldBe 2
            result.availableBoxes shouldBe 1
            result.availableRemainder shouldBe 1
            verify(exactly = 1) {
                inventory.saveMovement(match { it.movementType == "ADJUSTMENT" && it.quantityDelta == 5 && it.referenceType == "INITIAL_RECEIPT" })
            }
        }

        it("예약 재고보다 낮게 실재고를 조정하지 못한다") {
            every { inventory.lockedStock(skuEntity) } returns stock(10, 8)
            shouldThrow<IllegalArgumentException> { service.adjust(skuId, -3, "CORRECTION", null, null) }
        }
    }

    describe("재고 예약") {
        it("가용 재고보다 많은 수량은 예약하지 못한다") {
            val key = UUID.randomUUID()
            every { inventory.reservation(key) } returns null
            every { inventory.lockedStock(skuEntity) } returns stock(5, 2)
            shouldThrow<IllegalArgumentException> { service.reserve(skuId, 4, key, null) }
        }

        it("확정된 주문 취소 시 차감 재고를 복구하고 중복 복구는 멱등 처리한다") {
            val key = UUID.randomUUID()
            val reservation = StockReservationEntity().apply {
                reservationKey = key
                sku = skuEntity
                quantity = 2
                status = "CONFIRMED"
            }
            val entity = stock(8, 0)
            every { inventory.reservation(key) } returns reservation
            every { inventory.lockedStock(skuEntity) } returns entity
            every { inventory.stock(9L) } returns entity

            service.restoreConfirmed(key).onHand shouldBe 10
            service.restoreConfirmed(key).onHand shouldBe 10
            verify(exactly = 1) {
                inventory.saveMovement(match { it.movementType == "RESTOCK" && it.quantityDelta == 2 && it.referenceId == key })
            }
        }
    }
})
