package com.buyeong.umji.api.domain.shipment.service

import com.buyeong.umji.api.domain.inventory.service.InventoryService
import com.buyeong.umji.api.domain.notification.dto.NotificationEventType
import com.buyeong.umji.api.domain.notification.service.NotificationEventService
import com.buyeong.umji.api.domain.shipment.dto.ShipmentTrackingCandidateDto
import com.buyeong.umji.api.domain.shipment.integration.tracking.OfficialCarrierTrackingGateway
import com.buyeong.umji.api.domain.shipment.model.CarrierTrackingStatus
import com.buyeong.umji.api.domain.shipment.service.BrandShippingService
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderItemEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderPaymentEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderEntity
import com.buyeong.umji.api.persistence.jpa.order.service.OrderShipmentJpaEntityService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.util.UUID

class ShipmentServiceTest : DescribeSpec({
    val store = mockk<OrderShipmentJpaEntityService>(relaxed = true)
    val inventory = mockk<InventoryService>(relaxed = true)
    val tracking = mockk<OfficialCarrierTrackingGateway>(relaxed = true)
    val notifications = mockk<NotificationEventService>(relaxed = true)
    val brandShipping = mockk<BrandShippingService>(relaxed = true)
    val service = ShipmentService(store, inventory, tracking, notifications, brandShipping)
    val orderId = UUID.randomUUID()
    val reservation = UUID.randomUUID()
    fun entity(
        status: String,
        carrier: String? = null,
        trackingNumber: String? = null,
        paymentBeforeShipmentRequired: Boolean = false,
        paymentStatus: String = "WAITING_FOR_DEPOSIT",
    ): OrderShipmentEntity {
        val organization = mockk<OrganizationEntity>(relaxed = true)
        every { organization.paymentBeforeShipmentRequired } returns paymentBeforeShipmentRequired
        val payment = mockk<OrderPaymentEntity>(relaxed = true)
        every { payment.status } returns paymentStatus
        val order = mockk<PurchaseOrderEntity>(relaxed = true)
        every { order.publicId } returns orderId
        every { order.items } returns mutableListOf(mockk<OrderItemEntity>(relaxed = true) { every { reservationKey } returns reservation })
        every { order.organization } returns organization
        every { order.payment } returns payment
        return mockk<OrderShipmentEntity>(relaxed = true) {
            every { this@mockk.order } returns order
            every { this@mockk.status } returns status
            every { carrierCode } returns carrier
            every { this@mockk.trackingNumber } returns trackingNumber
        }
    }

    beforeTest {
        io.mockk.clearMocks(store, inventory, tracking, notifications)
    }

    it("배송 준비 배치에서 재고를 먼저 확정하고 PREPARING으로 변경한다") {
        val shipment = entity("READY_TO_SHIP")
        every { store.readyOrderIds() } returns listOf(orderId)
        every { store.findForUpdate(orderId) } returns shipment
        every { store.update(shipment, "PREPARING", null, null, null) } returns entity("PREPARING")
        service.prepareReadyOrders() shouldBe 1
        verifyOrder {
            inventory.confirmCost(reservation)
            store.update(shipment, "PREPARING", null, null, null)
        }
        verify(exactly = 1) { notifications.record(NotificationEventType.SHIPMENT_PREPARING, orderId, null) }
    }

    it("선결제 조직은 미결제와 부분 입금 상태에서 출고 준비와 재고 확정을 보류한다") {
        listOf("WAITING_FOR_DEPOSIT", "PARTIAL_PAYMENT_REVIEW_REQUIRED").forEach { paymentStatus ->
            val shipment = entity(
                "READY_TO_SHIP",
                paymentBeforeShipmentRequired = true,
                paymentStatus = paymentStatus,
            )
            every { store.findForUpdate(orderId) } returns shipment

            val result = service.prepareOrder(orderId)

            result.status shouldBe "READY_TO_SHIP"
            result.changed shouldBe false
        }

        verify(exactly = 0) { inventory.confirmCost(any()) }
        verify(exactly = 0) { store.update(any(), any(), any(), any(), any()) }
        verify(exactly = 0) { notifications.record(NotificationEventType.SHIPMENT_PREPARING, any(), any()) }
    }

    it("선결제 조직의 전액 입금 확인 후 다음 배송 준비에서 출고를 재개한다") {
        val shipment = entity(
            "READY_TO_SHIP",
            paymentBeforeShipmentRequired = true,
            paymentStatus = "PAYMENT_CONFIRMED",
        )
        every { store.findForUpdate(orderId) } returns shipment
        every { store.update(shipment, "PREPARING", null, null, null) } returns entity("PREPARING")

        val result = service.prepareOrder(orderId)

        result.status shouldBe "PREPARING"
        result.changed shouldBe true
        verify(exactly = 1) { inventory.confirmCost(reservation) }
        verify(exactly = 1) { store.update(shipment, "PREPARING", null, null, null) }
        verify(exactly = 1) { notifications.record(NotificationEventType.SHIPMENT_PREPARING, orderId, null) }
    }

    it("송장 등록 시 배송중 알림을 기록한다") {
        val shipment = entity("PREPARING")
        every { store.findForUpdate(orderId) } returns shipment
        every { store.update(any(), "IN_TRANSIT", "DAESIN", "1501602023302", any()) } returns entity("IN_TRANSIT", "DAESIN", "1501602023302")

        service.registerTracking(orderId, "DAESIN", "1501602023302", UUID.randomUUID()).changed shouldBe true

        verify(exactly = 1) { notifications.record(NotificationEventType.SHIPMENT_IN_TRANSIT, orderId, null) }
    }

    it("이미 등록된 동일 송장은 중복 알림을 기록하지 않는다") {
        every { store.findForUpdate(orderId) } returns entity("IN_TRANSIT", "DAESIN", "1501602023302")

        service.registerTracking(orderId, "DAESIN", "1501602023302", UUID.randomUUID()).changed shouldBe false

        verify(exactly = 0) { notifications.record(any(), any(), any()) }
    }

    it("배송완료 전이 시 한 번만 완료 알림을 기록한다") {
        every { store.findForUpdate(orderId) } returns entity("IN_TRANSIT", "DAESIN", "1501602023302")
        every { store.update(any(), "DELIVERED", "DAESIN", "1501602023302", any()) } returns entity("DELIVERED", "DAESIN", "1501602023302")

        service.markDelivered(orderId, UUID.randomUUID()).changed shouldBe true

        verify(exactly = 1) { notifications.record(NotificationEventType.SHIPMENT_DELIVERED, orderId, null) }
    }

    it("이미 완료된 주문은 완료 알림을 다시 기록하지 않는다") {
        every { store.findForUpdate(orderId) } returns entity("DELIVERED")

        service.markDelivered(orderId, UUID.randomUUID()).changed shouldBe false

        verify(exactly = 0) { notifications.record(any(), any(), any()) }
    }

    it("고객 주문 목록의 배송완료 송장은 DELIVERED로 전환한다") {
        val customerId = UUID.randomUUID()
        val candidate = ShipmentTrackingCandidateDto(orderId, UUID.randomUUID(), "IN_TRANSIT", "DAESIN", "1501602023302")
        every { store.trackingCandidatesForCustomer(customerId) } returns listOf(candidate)
        every { tracking.lookup("DAESIN", candidate.trackingNumber) } returns CarrierTrackingStatus.DELIVERED
        every { store.markDeliveredIfCurrent(candidate) } returns true

        val result = service.refreshForCustomer(customerId)

        result shouldBe 1
        verifyOrder {
            store.trackingCandidatesForCustomer(customerId)
            tracking.lookup("DAESIN", candidate.trackingNumber)
            store.markDeliveredIfCurrent(candidate)
        }
    }

    it("택배사 응답이 없으면 배송중 상태를 유지하고 다른 주문도 계속 조회한다") {
        val customerId = UUID.randomUUID()
        val candidate = ShipmentTrackingCandidateDto(orderId, UUID.randomUUID(), "IN_TRANSIT", "DAESIN", "1501602023302")
        val otherOrderId = UUID.randomUUID()
        val otherCandidate = ShipmentTrackingCandidateDto(otherOrderId, UUID.randomUUID(), "IN_TRANSIT", "CHUNIL", "72601701177")
        every { store.trackingCandidatesForCustomer(customerId) } returns listOf(candidate, otherCandidate)
        every { tracking.lookup("DAESIN", candidate.trackingNumber) } returns CarrierTrackingStatus.UNAVAILABLE
        every { tracking.lookup("CHUNIL", otherCandidate.trackingNumber) } returns CarrierTrackingStatus.DELIVERED
        every { store.markDeliveredIfCurrent(otherCandidate) } returns true

        val result = service.refreshForCustomer(customerId)

        result shouldBe 1
    }
})