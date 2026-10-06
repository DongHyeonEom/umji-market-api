package com.buyeong.umji.api.shipment.application

import com.buyeong.umji.api.shipment.adapter.out.tracking.KyoungdongTrackingAdapter
import com.buyeong.umji.api.shipment.application.model.CarrierTrackingStatus
import com.buyeong.umji.api.shipment.application.model.ShipmentTrackingCandidate
import com.buyeong.umji.api.shipment.application.port.out.ShipmentTrackingPort
import com.buyeong.umji.api.notification.application.NotificationEventService
import com.buyeong.umji.api.persistence.jpa.order.OrderShipmentJpaEntityService
import com.buyeong.umji.api.inventory.application.InventoryService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import io.mockk.every
import io.mockk.mockk
import java.util.UUID

class ShipmentServiceKyoungdongTrackingTest {
    @Test
    fun `customer order refresh marks a Kyoungdong shipment delivered from latest carrier scan`() {
        val candidate = ShipmentTrackingCandidate(
            orderId = UUID.randomUUID(),
            status = "IN_TRANSIT",
            carrierCode = "KDEXP",
            trackingNumber = "1501602023302",
        )
        val shipments = mockk<OrderShipmentJpaEntityService>()
        every { shipments.trackingCandidatesForCustomer(any()) } returns listOf(candidate)
        every { shipments.markDeliveredIfCurrent(candidate) } returns true
        val adapter = KyoungdongTrackingAdapter(jacksonObjectMapper())
        val tracking = object : ShipmentTrackingPort {
            override fun lookup(carrierCode: String, trackingNumber: String): CarrierTrackingStatus =
                adapter.parse(
                    """{"result":"suc","data":{"scanList":[{"scanTypeNm":"발송"},{"scanTypeNm":"도착"},{"scanTypeNm":"배송중"},{"scanTypeNm":"배송완료"}]}}""",
                )
        }
        val service = ShipmentService(
            shipments = shipments,
            inventory = mockk<InventoryService>(relaxed = true),
            tracking = tracking,
            notifications = mockk<NotificationEventService>(relaxed = true),
        )

        assertThat(service.refreshForCustomer(UUID.randomUUID())).isEqualTo(1)
    }
}
