package com.buyeong.umji.api.shipment.service

import com.buyeong.umji.api.shipment.integration.tracking.KyoungdongTrackingClient
import com.buyeong.umji.api.shipment.model.CarrierTrackingStatus
import com.buyeong.umji.api.shipment.model.ShipmentTrackingCandidate
import com.buyeong.umji.api.shipment.integration.tracking.OfficialCarrierTrackingGateway
import com.buyeong.umji.api.notification.service.NotificationEventService
import com.buyeong.umji.api.persistence.jpa.order.OrderShipmentJpaEntityService
import com.buyeong.umji.api.inventory.service.InventoryService
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
        val adapter = KyoungdongTrackingClient(jacksonObjectMapper())
        val tracking = mockk<OfficialCarrierTrackingGateway>()
        every { tracking.lookup(any(), any()) } returns adapter.parse(
            """{"result":"suc","data":{"scanList":[{"scanTypeNm":"발송"},{"scanTypeNm":"도착"},{"scanTypeNm":"배송중"},{"scanTypeNm":"배송완료"}]}}""",
        )
        val service = ShipmentService(
            shipments = shipments,
            inventory = mockk<InventoryService>(relaxed = true),
            tracking = tracking,
            notifications = mockk<NotificationEventService>(relaxed = true),
        )

        assertThat(service.refreshForCustomer(UUID.randomUUID())).isEqualTo(1)
    }
}
