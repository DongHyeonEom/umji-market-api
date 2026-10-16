package com.buyeong.umji.api.domain.operation.profit.service

import com.buyeong.umji.api.persistence.jpa.catalog.entity.BrandEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderItemEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentPackageEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentPackageItemEntity
import com.buyeong.umji.api.persistence.jpa.order.service.OrderProfitJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class OperationItemProfitServiceTest : DescribeSpec({
    val orderProfit = mockk<OrderProfitJpaEntityService>()
    val service = OperationItemProfitService(orderProfit)
    val skuId = UUID.randomUUID()
    val brandId = UUID.randomUUID()

    it("주문 품목 판매액에서 FIFO 사입원가와 송장 배부 운임을 차감한다") {
        val brandEntity = mockk<BrandEntity>()
        every { brandEntity.publicId } returns brandId
        every { brandEntity.name } returns "브랜드"
        val product = mockk<ProductEntity>()
        every { product.brand } returns brandEntity
        val sku = mockk<ProductSkuEntity>()
        every { sku.id } returns 10L
        every { sku.publicId } returns skuId
        every { sku.skuCode } returns "SKU-1"
        every { sku.name } returns "품목"
        every { sku.product } returns product
        val item = mockk<OrderItemEntity>()
        every { item.id } returns 20L
        every { item.sku } returns sku
        every { item.quantity } returns 2
        every { item.lineAmount } returns 500L
        every { item.purchaseCostSnapshot } returns 200L
        val packageLine = mockk<OrderShipmentPackageItemEntity> {
            every { orderItem } returns item
            every { boxQuantity } returns 2
            every { lineShippingCostSnapshot } returns 101L
        }
        val parcel = mockk<OrderShipmentPackageEntity> {
            every { items } returns mutableListOf(packageLine)
            every { actualShippingCost } returns 101L
        }
        every { orderProfit.orderItems(any(), any()) } returns listOf(item)
        every { orderProfit.packages(any(), any()) } returns listOf(parcel)

        val result = service.query(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), null, brandId)

        val row = result.items.single()
        result.from shouldBe "2026-01-01"
        result.until shouldBe "2026-01-31"
        row.skuId shouldBe skuId
        row.brandId shouldBe brandId
        row.orderItemCount shouldBe 1L
        row.completeCount shouldBe 1L
        row.incompleteCostCount shouldBe 0L
        row.incompleteShippingCount shouldBe 0L
        row.salesAmount shouldBe 500L
        row.purchaseCost shouldBe 200L
        row.shippingCost shouldBe 101L
        row.netProfit shouldBe 199L
    }

    it("조회 종료일이 시작일보다 빠르면 거부한다") {
        shouldThrow<IllegalArgumentException> {
            service.query(LocalDate.parse("2026-02-01"), LocalDate.parse("2026-01-01"), null, null)
        }
    }

    it("원가와 송장 품목 수량이 미확정인 주문 품목은 집계 완료액에서 제외한다") {
        val brand = mockk<BrandEntity> {
            every { publicId } returns brandId
            every { name } returns "브랜드"
        }
        val product = mockk<ProductEntity>()
        every { product.brand } returns brand
        val sku = mockk<ProductSkuEntity>()
        every { sku.id } returns 10L
        every { sku.publicId } returns skuId
        every { sku.skuCode } returns "SKU-1"
        every { sku.name } returns "품목"
        every { sku.product } returns product
        val missingCost = mockk<OrderItemEntity>()
        every { missingCost.id } returns 30L
        every { missingCost.sku } returns sku
        every { missingCost.quantity } returns 2
        every { missingCost.lineAmount } returns 800L
        every { missingCost.purchaseCostSnapshot } returns null
        val incompleteShipping = mockk<OrderItemEntity>()
        every { incompleteShipping.id } returns 31L
        every { incompleteShipping.sku } returns sku
        every { incompleteShipping.quantity } returns 2
        every { incompleteShipping.lineAmount } returns 600L
        every { incompleteShipping.purchaseCostSnapshot } returns 250L
        val partialPackageLine = mockk<OrderShipmentPackageItemEntity> {
            every { orderItem } returns incompleteShipping
            every { boxQuantity } returns 1
            every { lineShippingCostSnapshot } returns null
        }
        val parcel = mockk<OrderShipmentPackageEntity> {
            every { items } returns mutableListOf(partialPackageLine)
            every { actualShippingCost } returns null
        }
        every { orderProfit.orderItems(any(), any()) } returns listOf(missingCost, incompleteShipping)
        every { orderProfit.packages(any(), any()) } returns listOf(parcel)

        val row = service.query(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), null, null).items.single()

        row.orderItemCount shouldBe 2L
        row.completeCount shouldBe 0L
        row.incompleteCostCount shouldBe 1L
        row.incompleteShippingCount shouldBe 2L
        row.salesAmount shouldBe 0L
        row.purchaseCost shouldBe 0L
        row.shippingCost shouldBe 0L
    }

    it("품목·브랜드 필터와 한국 시간 주문일 범위를 적용하고 할인 후 line amount를 합산한다") {
        clearMocks(orderProfit)
        fun item(itemId: Long, targetSkuId: UUID, targetBrandId: UUID, code: String, amount: Long): OrderItemEntity {
            val targetBrand = mockk<BrandEntity> {
                every { publicId } returns targetBrandId
                every { name } returns code
            }
            val targetProduct = mockk<ProductEntity> { every { brand } returns targetBrand }
            val targetSku = mockk<ProductSkuEntity> {
                every { id } returns itemId
                every { publicId } returns targetSkuId
                every { skuCode } returns code
                every { name } returns code
                every { product } returns targetProduct
            }
            return mockk {
                every { id } returns itemId
                every { sku } returns targetSku
                every { quantity } returns 1
                every { lineAmount } returns amount
                every { purchaseCostSnapshot } returns 10L
            }
        }

        val otherSkuId = UUID.randomUUID()
        val otherBrandId = UUID.randomUUID()
        val discountedItem = item(40L, skuId, brandId, "SKU-1", 450L)
        val otherItem = item(41L, otherSkuId, otherBrandId, "SKU-2", 900L)
        val discountedPackageLine = mockk<OrderShipmentPackageItemEntity> {
            every { orderItem } returns discountedItem
            every { boxQuantity } returns 1
            every { lineShippingCostSnapshot } returns 0L
        }
        val parcel = mockk<OrderShipmentPackageEntity> {
            every { items } returns mutableListOf(discountedPackageLine)
            every { actualShippingCost } returns 0L
        }
        val from = slot<Instant>()
        val until = slot<Instant>()
        every { orderProfit.orderItems(capture(from), capture(until)) } returns listOf(discountedItem, otherItem)
        every { orderProfit.packages(any(), any()) } returns listOf(parcel)

        val skuFiltered = service.query(LocalDate.parse("2026-10-16"), LocalDate.parse("2026-10-16"), skuId, null)
        val brandFiltered = service.query(LocalDate.parse("2026-10-16"), LocalDate.parse("2026-10-16"), null, brandId)

        skuFiltered.items.map { it.skuId } shouldBe listOf(skuId)
        skuFiltered.items.single().salesAmount shouldBe 450L
        skuFiltered.items.single().netProfit shouldBe 440L
        brandFiltered.items.map { it.skuId } shouldBe listOf(skuId)
        from.captured shouldBe Instant.parse("2026-10-15T15:00:00Z")
        until.captured shouldBe Instant.parse("2026-10-16T15:00:00Z")
        verify(exactly = 2) { orderProfit.orderItems(any(), any()) }
    }
})
