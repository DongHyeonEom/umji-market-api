package com.buyeong.umji.api.shipment.application

import com.buyeong.umji.api.notification.application.model.NotificationEventType
import com.buyeong.umji.api.notification.application.port.`in`.NotificationEventUseCase
import com.buyeong.umji.api.shipment.application.model.CarrierTrackingStatus
import com.buyeong.umji.api.shipment.application.model.ShipmentRecord
import com.buyeong.umji.api.shipment.application.model.ShipmentTrackingCandidate
import com.buyeong.umji.api.shipment.application.port.out.ShipmentInventoryPort
import com.buyeong.umji.api.shipment.application.port.out.ShipmentStorePort
import com.buyeong.umji.api.shipment.application.port.out.ShipmentTrackingPort
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.util.UUID

class ShipmentServiceTest : DescribeSpec({
    val store = mockk<ShipmentStorePort>(relaxed = true)
    val inventory = mockk<ShipmentInventoryPort>(relaxed = true)
    val tracking = mockk<ShipmentTrackingPort>(relaxed = true)
    val notifications = mockk<NotificationEventUseCase>(relaxed = true)
    val service = ShipmentService(store, inventory, tracking, notifications)
    val orderId = UUID.randomUUID()
    val reservation = UUID.randomUUID()
    val record = ShipmentRecord(orderId, "READY_TO_SHIP", null, null, listOf(reservation))

    beforeTest {
        io.mockk.clearMocks(store, inventory, tracking, notifications)
    }

    it("배송 준비 배치에서 재고를 먼저 확정하고 PREPARING으로 변경한다") {
        every { store.readyOrderIds() } returns listOf(orderId)
        every { store.lock(orderId) } returns record
        every { store.update(record, "PREPARING", null, null, null) } returns record.copy(status = "PREPARING")
        service.prepareReadyOrders() shouldBe 1
        verifyOrder {
            inventory.confirm(reservation)
            store.update(record, "PREPARING", null, null, null)
        }
        verify(exactly = 1) { notifications.record(NotificationEventType.SHIPMENT_PREPARING, orderId, null) }
    }

    it("송장 등록 시 배송중 알림을 기록한다") {
        every { store.lock(orderId) } returns record.copy(status = "PREPARING")
        every { store.update(any(), "IN_TRANSIT", "DAESIN", "1501602023302", any()) } returns
            record.copy(status = "IN_TRANSIT", carrierCode = "DAESIN", trackingNumber = "1501602023302")

        service.registerTracking(orderId, "DAESIN", "1501602023302", UUID.randomUUID()).changed shouldBe true

        verify(exactly = 1) { notifications.record(NotificationEventType.SHIPMENT_IN_TRANSIT, orderId, null) }
    }

    it("이미 등록된 동일 송장은 중복 알림을 기록하지 않는다") {
        every { store.lock(orderId) } returns record.copy(status = "IN_TRANSIT", carrierCode = "DAESIN", trackingNumber = "1501602023302")

        service.registerTracking(orderId, "DAESIN", "1501602023302", UUID.randomUUID()).changed shouldBe false

        verify(exactly = 0) { notifications.record(any(), any(), any()) }
    }

    it("배송완료 전이 시 한 번만 완료 알림을 기록한다") {
        every { store.lock(orderId) } returns record.copy(status = "IN_TRANSIT", carrierCode = "DAESIN", trackingNumber = "1501602023302")
        every { store.update(any(), "DELIVERED", "DAESIN", "1501602023302", any()) } returns
            record.copy(status = "DELIVERED", carrierCode = "DAESIN", trackingNumber = "1501602023302")

        service.markDelivered(orderId, UUID.randomUUID()).changed shouldBe true

        verify(exactly = 1) { notifications.record(NotificationEventType.SHIPMENT_DELIVERED, orderId, null) }
    }

    it("이미 완료된 주문은 완료 알림을 다시 기록하지 않는다") {
        every { store.lock(orderId) } returns record.copy(status = "DELIVERED")

        service.markDelivered(orderId, UUID.randomUUID()).changed shouldBe false

        verify(exactly = 0) { notifications.record(any(), any(), any()) }
    }

    it("고객 주문 목록의 배송완료 송장은 DELIVERED로 전환한다") {
        val customerId = UUID.randomUUID()
        val candidate = ShipmentTrackingCandidate(orderId, "IN_TRANSIT", "DAESIN", "1501602023302")
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
        val candidate = ShipmentTrackingCandidate(orderId, "IN_TRANSIT", "DAESIN", "1501602023302")
        val otherOrderId = UUID.randomUUID()
        val otherCandidate = ShipmentTrackingCandidate(otherOrderId, "IN_TRANSIT", "CHUNIL", "72601701177")
        every { store.trackingCandidatesForCustomer(customerId) } returns listOf(candidate, otherCandidate)
        every { tracking.lookup("DAESIN", candidate.trackingNumber) } returns CarrierTrackingStatus.UNAVAILABLE
        every { tracking.lookup("CHUNIL", otherCandidate.trackingNumber) } returns CarrierTrackingStatus.DELIVERED
        every { store.markDeliveredIfCurrent(otherCandidate) } returns true

        val result = service.refreshForCustomer(customerId)

        result shouldBe 1
    }
})