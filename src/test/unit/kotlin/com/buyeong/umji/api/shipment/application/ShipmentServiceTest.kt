package com.buyeong.umji.api.shipment.application

import com.buyeong.umji.api.shipment.application.model.ShipmentRecord
import com.buyeong.umji.api.shipment.application.port.out.ShipmentInventoryPort
import com.buyeong.umji.api.shipment.application.port.out.ShipmentStorePort
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import java.util.UUID

class ShipmentServiceTest : DescribeSpec({
    val store = mockk<ShipmentStorePort>(relaxed = true)
    val inventory = mockk<ShipmentInventoryPort>(relaxed = true)
    val service = ShipmentService(store, inventory)
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
})