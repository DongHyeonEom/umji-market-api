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
import io.mockk.mockk
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
            every { orderItem } returns item; every { boxQuantity } returns 2; every { lineShippingCostSnapshot } returns 101L
        }
        val parcel = mockk<OrderShipmentPackageEntity> {
            every { items } returns mutableListOf(packageLine); every { actualShippingCost } returns 101L
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
})
