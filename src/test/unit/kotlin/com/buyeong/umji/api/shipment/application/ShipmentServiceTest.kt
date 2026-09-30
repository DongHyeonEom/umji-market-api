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

    it("고객이 배송 조회를 요청하고 배송완료 결과를 받으면 주문 상태를 DELIVERED로 전환한다") {
        val customerId = UUID.randomUUID()
        val candidate = ShipmentTrackingCandidate(orderId, "IN_TRANSIT", "DAESIN", "1501602023302")
        every { store.trackingCandidateForCustomer(orderId, customerId) } returns candidate
        every { tracking.lookup("DAESIN", candidate.trackingNumber) } returns CarrierTrackingStatus.DELIVERED
        every { store.markDeliveredIfCurrent(candidate) } returns true

        val result = service.refreshForCustomer(orderId, customerId)

        result.status shouldBe "DELIVERED"
        result.changed shouldBe true
    }

    it("매시간 배송 조회에서 배송완료 결과만 상태를 전환한다") {
        val candidate = ShipmentTrackingCandidate(orderId, "IN_TRANSIT", "DAESIN", "1501602023302")
        every { store.trackingCandidates() } returns listOf(candidate)
        every { tracking.lookup("DAESIN", candidate.trackingNumber) } returns CarrierTrackingStatus.DELIVERED
        every { store.markDeliveredIfCurrent(candidate) } returns true

        service.synchronizeTrackingStatus() shouldBe 1
    }

    it("택배사 응답이 없으면 배송중 상태를 유지한다") {
        val customerId = UUID.randomUUID()
        val candidate = ShipmentTrackingCandidate(orderId, "IN_TRANSIT", "DAESIN", "1501602023302")
        val current = ShipmentRecord(orderId, "IN_TRANSIT", "DAESIN", candidate.trackingNumber, emptyList())
        every { store.trackingCandidateForCustomer(orderId, customerId) } returns candidate
        every { tracking.lookup("DAESIN", candidate.trackingNumber) } returns CarrierTrackingStatus.UNAVAILABLE
        every { store.lock(orderId) } returns current

        val result = service.refreshForCustomer(orderId, customerId)

        result.status shouldBe "IN_TRANSIT"
        result.changed shouldBe false
    }
})