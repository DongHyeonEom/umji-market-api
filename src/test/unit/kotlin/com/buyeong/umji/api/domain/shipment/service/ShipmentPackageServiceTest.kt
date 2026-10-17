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
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentPackageEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentPackageItemEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderEntity
import com.buyeong.umji.api.persistence.jpa.order.service.OrderShipmentJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
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
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
            operatorId,
            "",
            listOf(SimpleGrantedAuthority("SHIPMENT_ASSIGN")),
        )
    }

    afterTest {
        SecurityContextHolder.clearContext()
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
            shipments,
            mockk<InventoryService>(relaxed = true),
            mockk<OfficialCarrierTrackingGateway>(relaxed = true),
            mockk<NotificationEventService>(relaxed = true),
            brandShipping,
        )
        val first = service.registerPackage(
            orderId,
            brandId,
            ShipmentPackageRequest(
                "DAESIN",
                "TRACK-1",
                actualShippingCost = 2201,
                items = listOf(ShipmentPackageItemRequest(orderItemId, 3), ShipmentPackageItemRequest(noRateItemId, 1))
            ),
            operatorId,
        )
        val second = service.registerPackage(
            orderId,
            brandId,
            ShipmentPackageRequest("DAESIN", "TRACK-2", items = listOf(ShipmentPackageItemRequest(orderItemId, 2))),
            operatorId,
        )
        val manualCost = service.registerPackage(
            orderId,
            brandId,
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

    it("복수 브랜드 주문을 브랜드별 송장으로 분리하고 각 기본 운송비를 적용한다") {
        val otherBrandId = UUID.randomUUID()
        val otherSkuId = UUID.randomUUID()
        val otherOrderItemId = UUID.randomUUID()
        val firstItem = orderItem()
        val secondItem = orderItem(otherBrandId, otherSkuId, otherOrderItemId)
        val orderEntity = mockk<PurchaseOrderEntity>(relaxed = true) {
            every { publicId } returns orderId
            every { items } returns mutableListOf(firstItem, secondItem)
        }
        val shipment = mockk<OrderShipmentEntity>(relaxed = true) {
            every { status } returns "PREPARING"
            every { order } returns orderEntity
        }
        every { shipments.findForUpdate(orderId) } returns shipment
        every { shipments.allocatedBoxQuantity(any()) } returns 0
        every { brandShipping.canManageBrandOrAdmin(any(), operatorId) } returns true
        every { brandShipping.rate(brandId, skuId, "DAESIN") } returns BrandShippingRateDto(brandId, skuId, "SKU", "DAESIN", 700)
        every { brandShipping.rate(otherBrandId, otherSkuId, "DAESIN") } returns BrandShippingRateDto(otherBrandId, otherSkuId, "SKU", "DAESIN", 900)
        every { shipments.registerPackage(shipment, any(), operatorId) } answers {
            secondArg<OrderShipmentPackageEntity>().apply {
                this.shipment = shipment
                publicId = UUID.randomUUID()
            }
        }
        val service = ShipmentService(
            shipments,
            mockk<InventoryService>(relaxed = true),
            mockk<OfficialCarrierTrackingGateway>(relaxed = true),
            mockk<NotificationEventService>(relaxed = true),
            brandShipping,
        )

        val firstPackage = service.registerPackage(
            orderId,
            brandId,
            ShipmentPackageRequest("DAESIN", "TRACK-A", items = listOf(ShipmentPackageItemRequest(orderItemId, 1))),
            operatorId,
        )
        val secondPackage = service.registerPackage(
            orderId,
            otherBrandId,
            ShipmentPackageRequest("DAESIN", "TRACK-B", items = listOf(ShipmentPackageItemRequest(otherOrderItemId, 2))),
            operatorId,
        )

        firstPackage.brandId shouldBe brandId
        firstPackage.actualShippingCost shouldBe 700L
        secondPackage.brandId shouldBe otherBrandId
        secondPackage.actualShippingCost shouldBe 1800L
        verify(exactly = 2) { shipments.registerPackage(shipment, any(), operatorId) }
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
            shipments,
            mockk<InventoryService>(relaxed = true),
            mockk<OfficialCarrierTrackingGateway>(relaxed = true),
            mockk<NotificationEventService>(relaxed = true),
            brandShipping,
        )
        shouldThrow<IllegalArgumentException> {
            service.registerPackage(
                orderId,
                brandId,
                ShipmentPackageRequest(
                    "DAESIN",
                    "TRACK-2",
                    1000,
                    listOf(
                        ShipmentPackageItemRequest(orderItemId, 1),
                        ShipmentPackageItemRequest(secondItem, 1),
                    )
                ),
                operatorId,
            )
        }
        verify(exactly = 0) { shipments.registerPackage(any(), any(), any()) }
    }

    it("실제 운송비 변경을 저장하고 품목 snapshot에 원 단위 차액을 반영한다") {
        val firstItem = orderItem()
        val secondItemId = UUID.randomUUID()
        val secondItem = orderItem(itemSkuId = UUID.randomUUID(), itemId = secondItemId)
        val order = mockk<PurchaseOrderEntity> { every { publicId } returns orderId }
        val shipment = mockk<OrderShipmentEntity>()
        every { shipment.order } returns order
        val firstLine = OrderShipmentPackageItemEntity().apply {
            orderItem = firstItem
            boxQuantity = 1
        }
        val secondLine = OrderShipmentPackageItemEntity().apply {
            orderItem = secondItem
            boxQuantity = 1
        }
        val parcel = OrderShipmentPackageEntity().apply {
            publicId = UUID.randomUUID()
            this.shipment = shipment
            brand = mockk<BrandEntity> { every { publicId } returns brandId }
            carrierCode = "DAESIN"
            trackingNumber = "TRACK-COST"
            items = mutableListOf(firstLine, secondLine)
        }
        every { shipments.findPackageForUpdate(requireNotNull(parcel.publicId)) } returns parcel
        every { brandShipping.canManageBrandOrAdmin(brandId, operatorId) } returns true
        every { shipments.updateActualCost(parcel, 101L, operatorId) } answers {
            firstArg<OrderShipmentPackageEntity>().apply { actualShippingCost = 101L }
        }

        val service = ShipmentService(
            shipments,
            mockk<InventoryService>(relaxed = true),
            mockk<OfficialCarrierTrackingGateway>(relaxed = true),
            mockk<NotificationEventService>(relaxed = true),
            brandShipping,
        )
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
            operatorId,
            "",
            listOf(SimpleGrantedAuthority("SHIPMENT_WRITE")),
        )
        shouldThrow<AccessDeniedException> {
            service.updateActualCost(requireNotNull(parcel.publicId), 101L, operatorId, "작업자 수기 변경")
        }
        verify(exactly = 0) { shipments.updateActualCost(parcel, any(), operatorId) }
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
            operatorId,
            "",
            listOf(SimpleGrantedAuthority("SHIPMENT_ASSIGN")),
        )
        val result = service.updateActualCost(requireNotNull(parcel.publicId), 101L, operatorId, "택배사 실청구")

        result.actualShippingCost shouldBe 101L
        parcel.items.map { it.lineShippingCostSnapshot } shouldBe listOf(51L, 50L)
        verify(exactly = 1) { shipments.updateActualCost(parcel, 101L, operatorId) }
    }

    it("배정되지 않은 브랜드 담당자는 송장을 등록하지 못한다") {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
            operatorId,
            "",
            listOf(SimpleGrantedAuthority("SHIPMENT_WRITE")),
        )
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
            shipments,
            mockk<InventoryService>(relaxed = true),
            mockk<OfficialCarrierTrackingGateway>(relaxed = true),
            mockk<NotificationEventService>(relaxed = true),
            brandShipping,
        )
        shouldThrow<AccessDeniedException> {
            service.registerPackage(
                orderId,
                brandId,
                ShipmentPackageRequest("DAESIN", "TRACK-3", 1000, listOf(ShipmentPackageItemRequest(orderItemId, 1))),
                operatorId,
            )
        }
        verify(exactly = 0) { brandShipping.rate(any(), any(), any()) }
        verify(exactly = 0) { shipments.registerPackage(any(), any(), any()) }
    }

    it("SHIPMENT_WRITE는 설정된 요율만 사용하며 수기 비용과 요율 누락을 거부한다") {
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
        every { brandShipping.canManageBrandOrAdmin(brandId, operatorId) } returns true
        every { brandShipping.rate(brandId, skuId, "DAESIN") } returns BrandShippingRateDto(brandId, skuId, "SKU", "DAESIN", 700)
        every { shipments.registerPackage(shipment, any(), operatorId) } answers {
            secondArg<OrderShipmentPackageEntity>().apply {
                this.shipment = shipment
                publicId = UUID.randomUUID()
            }
        }
        val service = ShipmentService(
            shipments,
            mockk<InventoryService>(relaxed = true),
            mockk<OfficialCarrierTrackingGateway>(relaxed = true),
            mockk<NotificationEventService>(relaxed = true),
            brandShipping,
        )
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
            operatorId,
            "",
            listOf(SimpleGrantedAuthority("SHIPMENT_WRITE")),
        )

        val saved = service.registerPackage(
            orderId,
            brandId,
            ShipmentPackageRequest("DAESIN", "TRACK-WRITE", items = listOf(ShipmentPackageItemRequest(orderItemId, 2))),
            operatorId,
        )
        saved.actualShippingCost shouldBe 1400L
        shouldThrow<IllegalArgumentException> {
            service.registerPackage(
                orderId,
                brandId,
                ShipmentPackageRequest("DAESIN", "TRACK-MANUAL", actualShippingCost = 1200, items = listOf(ShipmentPackageItemRequest(orderItemId, 1))),
                operatorId,
            )
        }

        every { brandShipping.rate(brandId, skuId, "DAESIN") } returns null
        shouldThrow<IllegalArgumentException> {
            service.registerPackage(
                orderId,
                brandId,
                ShipmentPackageRequest("DAESIN", "TRACK-NO-RATE", items = listOf(ShipmentPackageItemRequest(orderItemId, 1))),
                operatorId,
            )
        }
        verify(exactly = 1) { shipments.registerPackage(shipment, any(), operatorId) }
    }

    it("관리자는 전체 송장을 조회하고 배송 담당자는 배정 브랜드로 조회 범위가 제한된다") {
        val service = ShipmentService(
            shipments,
            mockk<InventoryService>(relaxed = true),
            mockk<OfficialCarrierTrackingGateway>(relaxed = true),
            mockk<NotificationEventService>(relaxed = true),
            brandShipping,
        )
        val assignedBrands = setOf(brandId)
        every { brandShipping.assignedBrandIds(operatorId) } returns assignedBrands
        every { shipments.operationPackages(any(), any()) } returns emptyList()

        try {
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
                operatorId,
                "",
                listOf(SimpleGrantedAuthority("SHIPMENT_ASSIGN")),
            )
            service.operationPackages(operatorId)
            verify { shipments.operationPackages(assignedBrands, true) }

            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken.authenticated(
                operatorId,
                "",
                listOf(SimpleGrantedAuthority("SHIPMENT_READ")),
            )
            service.operationPackages(operatorId)
            verify { shipments.operationPackages(assignedBrands, false) }
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
})
