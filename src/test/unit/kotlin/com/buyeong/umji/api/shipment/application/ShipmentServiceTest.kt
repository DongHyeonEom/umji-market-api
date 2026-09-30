package com.buyeong.umji.api.shipment.application

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
import io.mockk.verifyOrder
import java.util.UUID

class ShipmentServiceTest : DescribeSpec({
    val store = mockk<ShipmentStorePort>(relaxed = true)
    val inventory = mockk<ShipmentInventoryPort>(relaxed = true)
    val tracking = mockk<ShipmentTrackingPort>(relaxed = true)
    val service = ShipmentService(store, inventory, tracking)
    val orderId = UUID.randomUUID()
    val reservation = UUID.randomUUID()
    val record = ShipmentRecord(orderId, "READY_TO_SHIP", null, null, listOf(reservation))

    it("배송 준비 배치에서 재고를 먼저 확정하고 PREPARING으로 변경한다") {
        every { store.readyOrderIds() } returns listOf(orderId)
        every { store.lock(orderId) } returns record
        every { store.update(record, "PREPARING", null, null, null) } returns record.copy(status = "PREPARING")
        service.prepareReadyOrders() shouldBe 1
        verifyOrder {
            inventory.confirm(reservation)
            store.update(record, "PREPARING", null, null, null)
        }
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