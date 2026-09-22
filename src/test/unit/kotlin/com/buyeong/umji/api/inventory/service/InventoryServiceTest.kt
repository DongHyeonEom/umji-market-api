package com.buyeong.umji.api.inventory.service

import com.buyeong.umji.api.persistence.jpa.catalog.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import com.buyeong.umji.api.inventory.model.AdjustInventoryRequest
import com.buyeong.umji.api.persistence.jpa.inventory.InventoryMovementEntity
import com.buyeong.umji.api.persistence.jpa.inventory.InventoryJpaEntityService
import com.buyeong.umji.api.persistence.jpa.inventory.InventoryStockEntity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class InventoryServiceTest : DescribeSpec({
    val productSkus = mockk<CatalogJpaEntityService>()
    val inventory = mockk<InventoryJpaEntityService>(relaxed = true)
    val service = InventoryService(productSkus, inventory)
    val skuId = UUID.randomUUID()
    val sku = mockk<ProductSkuEntity>()

    beforeTest {
        every { sku.id } returns 1L
        every { sku.publicId } returns skuId
        every { sku.skuCode } returns "SKU-001"
        every { productSkus.sku(skuId) } returns sku
        every { inventory.saveMovement(any<InventoryMovementEntity>()) } answers { firstArg() }
    }

    describe("운영 재고 조정") {
        it("실재고를 조정하고 변동 이력을 남긴다") {
            val stock = InventoryStockEntity().apply {
                this.sku = sku
                onHandQuantity = 10
                reservedQuantity = 2
            }
            every { inventory.lockedStock(sku) } returns stock

            val response = service.adjust(skuId, AdjustInventoryRequest(5, "INITIAL_RECEIPT", "입고"))

            response.onHandQuantity shouldBe 15
            response.availableQuantity shouldBe 13
            verify(exactly = 1) { inventory.saveMovement(any()) }
        }

        it("예약 재고보다 낮게 실재고를 조정하지 못한다") {
            val stock = InventoryStockEntity().apply {
                this.sku = sku
                onHandQuantity = 10
                reservedQuantity = 8
            }
            every { inventory.lockedStock(sku) } returns stock

            shouldThrow<IllegalArgumentException> {
                service.adjust(skuId, AdjustInventoryRequest(-3, "CORRECTION"))
            }
        }
    }

    describe("재고 예약") {
        it("가용 재고보다 많은 수량은 예약하지 못한다") {
            val stock = InventoryStockEntity().apply {
                this.sku = sku
                onHandQuantity = 5
                reservedQuantity = 2
            }
            every { inventory.lockedStock(sku) } returns stock
            every { inventory.reservation(any()) } returns null

            shouldThrow<IllegalArgumentException> {
                service.reserve(skuId, 4, UUID.randomUUID(), null)
            }
        }
    }
})
