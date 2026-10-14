package com.buyeong.umji.api.domain.shipment.service

import com.buyeong.umji.api.domain.inventory.service.InventoryService
import com.buyeong.umji.api.domain.notification.service.NotificationEventService
import com.buyeong.umji.api.domain.shipment.dto.BrandShippingRateDto
import com.buyeong.umji.api.domain.shipment.integration.tracking.OfficialCarrierTrackingGateway
import com.buyeong.umji.api.domain.shipment.model.ShipmentPackageItemRequest
import com.buyeong.umji.api.domain.shipment.model.ShipmentPackageRequest
import com.buyeong.umji.api.persistence.jpa.catalog.entity.BrandEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderItemEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderEntity
import com.buyeong.umji.api.persistence.jpa.order.service.OrderShipmentJpaEntityService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class ShipmentPackageServiceTest : DescribeSpec({
    val shipments = mockk<OrderShipmentJpaEntityService>(relaxed = true)
    val brandShipping = mockk<BrandShippingService>(relaxed = true)
    val orderId = UUID.randomUUID()
    val brandId = UUID.randomUUID()
    val skuId = UUID.randomUUID()
    val orderItemId = UUID.randomUUID()
    val operatorId = UUID.randomUUID()

    fun orderItem(itemBrandId: UUID = brandId, itemSkuId: UUID = skuId, itemId: UUID = orderItemId): OrderItemEntity {
        val brandEntity = mockk<BrandEntity> {
            every { id } returns if (itemBrandId == brandId) 1L else 2L
            every { publicId } returns itemBrandId
        }
        val productEntity = mockk<ProductEntity> { every { brand } returns brandEntity }
        val skuEntity = mockk<ProductSkuEntity> {
            every { publicId } returns itemSkuId
            every { product } returns productEntity
        }
        return mockk<OrderItemEntity> {
            every { id } returns if (itemId == orderItemId) 1L else 2L
            every { publicId } returns itemId
            every { sku } returns skuEntity
            every { skuName } returns "SKU"
            every { quantity } returns 5
        }
    }

    beforeTest {
        io.mockk.clearMocks(shipments, brandShipping)
    }

    it("박스 수량에 기본 운송비를 곱해 송장 비용과 품목별 snapshot을 저장한다") {
        val item = orderItem()
        val noRateItemId = UUID.randomUUID()
        val noRateSkuId = UUID.randomUUID()
        val noRateItem = orderItem(itemSkuId = noRateSkuId, itemId = noRateItemId)
        val orderEntity = mockk<PurchaseOrderEntity>(relaxed = true) {
            every { publicId } returns orderId
            every { items } returns mutableListOf(item, noRateItem)
        }
        val shipment = mockk<OrderShipmentEntity>(relaxed = true) {
            every { status } returns "PREPARING"
            every { order } returns orderEntity
        }
        val savedPackages = mutableListOf<com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentPackageEntity>()
        every { shipments.findForUpdate(orderId) } returns shipment
        every { shipments.allocatedBoxQuantity(orderItemId) } returnsMany listOf(0, 3)
        every { shipments.allocatedBoxQuantity(noRateItemId) } returns 0
        every { brandShipping.canManageBrandOrAdmin(brandId, operatorId) } returns true
        every { brandShipping.rate(brandId, skuId, "DAESIN") } returnsMany listOf(
            BrandShippingRateDto(brandId, skuId, "SKU", "DAESIN", 700),
            BrandShippingRateDto(brandId, skuId, "SKU", "DAESIN", 900),
        )
        every { brandShipping.rate(brandId, noRateSkuId, "DAESIN") } returns null
        every { shipments.registerPackage(shipment, any(), operatorId) } answers {
            secondArg<com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentPackageEntity>().apply {
                this.shipment = shipment
                publicId = UUID.randomUUID()
                savedPackages.add(this)
            }
        }

        val service = ShipmentService(
            shipments, mockk<InventoryService>(relaxed = true), mockk<OfficialCarrierTrackingGateway>(relaxed = true),
            mockk<NotificationEventService>(relaxed = true), brandShipping,
        )
        val first = service.registerPackage(
            orderId, brandId,
            ShipmentPackageRequest("DAESIN", "TRACK-1", actualShippingCost = 2201, items = listOf(ShipmentPackageItemRequest(orderItemId, 3), ShipmentPackageItemRequest(noRateItemId, 1))),
            operatorId,
        )
        val second = service.registerPackage(
            orderId, brandId,
            ShipmentPackageRequest("DAESIN", "TRACK-2", items = listOf(ShipmentPackageItemRequest(orderItemId, 2))),
            operatorId,
        )
        val manualCost = service.registerPackage(
            orderId, brandId,
            ShipmentPackageRequest("DAESIN", "TRACK-3", actualShippingCost = 500, items = listOf(ShipmentPackageItemRequest(noRateItemId, 1))),
            operatorId,
        )

        first.actualShippingCost shouldBe 2201L
        second.actualShippingCost shouldBe 1800L
        manualCost.actualShippingCost shouldBe 500L
        savedPackages.map { it.items.sumOf { line -> line.boxQuantity } } shouldBe listOf(4, 2, 1)
        savedPackages.first().items.first().amountPerBoxSnapshot shouldBe 700L
        savedPackages.first().items.map { it.lineShippingCostSnapshot } shouldBe listOf(1101L, 1100L)
        savedPackages[1].items.single().amountPerBoxSnapshot shouldBe 900L
        savedPackages.last().items.single().lineShippingCostSnapshot shouldBe 500L
        verify(exactly = 3) { shipments.registerPackage(shipment, any(), operatorId) }
    }

    it("다른 브랜드 품목이 섞인 송장은 등록하지 않는다") {
        val secondBrand = UUID.randomUUID()
        val secondSku = UUID.randomUUID()
        val secondItem = UUID.randomUUID()
        val orderEntity = mockk<PurchaseOrderEntity>(relaxed = true) {
            every { publicId } returns orderId
            every { items } returns mutableListOf(orderItem(), orderItem(secondBrand, secondSku, secondItem))
        }
        val shipment = mockk<OrderShipmentEntity>(relaxed = true) {
            every { status } returns "PREPARING"
            every { order } returns orderEntity
        }
        every { shipments.findForUpdate(orderId) } returns shipment
        every { shipments.allocatedBoxQuantity(any()) } returns 0

        val service = ShipmentService(
            shipments, mockk<InventoryService>(relaxed = true), mockk<OfficialCarrierTrackingGateway>(relaxed = true),
            mockk<NotificationEventService>(relaxed = true), brandShipping,
        )
        shouldThrow<IllegalArgumentException> {
            service.registerPackage(
                orderId, brandId,
                ShipmentPackageRequest("DAESIN", "TRACK-2", 1000, listOf(
                    ShipmentPackageItemRequest(orderItemId, 1), ShipmentPackageItemRequest(secondItem, 1),
                )),
                operatorId,
            )
        }
        verify(exactly = 0) { shipments.registerPackage(any(), any(), any()) }
    }

    it("배정되지 않은 브랜드 담당자는 송장을 등록하지 못한다") {
        val item = orderItem()
        val orderEntity = mockk<PurchaseOrderEntity>(relaxed = true) {
            every { publicId } returns orderId
            every { items } returns mutableListOf(item)
        }
        val shipment = mockk<OrderShipmentEntity>(relaxed = true) {
            every { status } returns "PREPARING"
            every { order } returns orderEntity
        }
        every { shipments.findForUpdate(orderId) } returns shipment
        every { shipments.allocatedBoxQuantity(orderItemId) } returns 0
        every { brandShipping.canManageBrandOrAdmin(brandId, operatorId) } returns false

        val service = ShipmentService(
            shipments, mockk<InventoryService>(relaxed = true), mockk<OfficialCarrierTrackingGateway>(relaxed = true),
            mockk<NotificationEventService>(relaxed = true), brandShipping,
        )
        shouldThrow<AccessDeniedException> {
            service.registerPackage(
                orderId, brandId,
                ShipmentPackageRequest("DAESIN", "TRACK-3", 1000, listOf(ShipmentPackageItemRequest(orderItemId, 1))),
                operatorId,
            )
        }
        verify(exactly = 0) { brandShipping.rate(any(), any(), any()) }
        verify(exactly = 0) { shipments.registerPackage(any(), any(), any()) }
    }

    it("관리자는 전체 송장을 조회하고 배송 담당자는 배정 브랜드로 조회 범위가 제한된다") {
        val service = ShipmentService(
            shipments, mockk<InventoryService>(relaxed = true), mockk<OfficialCarrierTrackingGateway>(relaxed = true),
            mockk<NotificationEventService>(relaxed = true), brandShipping,
        )
        val assignedBrands = setOf(brandId)
        every { brandShipping.assignedBrandIds(operatorId) } returns assignedBrands
        every { shipments.operationPackages(any(), any()) } returns emptyList()

        try {
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
                operatorId, "", listOf(SimpleGrantedAuthority("SHIPMENT_ASSIGN")),
            )
            service.operationPackages(operatorId)
            verify { shipments.operationPackages(assignedBrands, true) }

            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
                operatorId, "", listOf(SimpleGrantedAuthority("SHIPMENT_READ")),
            )
            service.operationPackages(operatorId)
            verify { shipments.operationPackages(assignedBrands, false) }
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
})
