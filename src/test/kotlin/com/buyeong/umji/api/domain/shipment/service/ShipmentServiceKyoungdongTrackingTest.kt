package com.buyeong.umji.api.domain.shipment.service

import com.buyeong.umji.api.domain.inventory.service.InventoryService
import com.buyeong.umji.api.domain.notification.service.NotificationEventService
import com.buyeong.umji.api.domain.shipment.dto.ShipmentTrackingCandidateDto
import com.buyeong.umji.api.domain.shipment.integration.tracking.KyoungdongTrackingClient
import com.buyeong.umji.api.domain.shipment.integration.tracking.OfficialCarrierTrackingGateway
import com.buyeong.umji.api.persistence.jpa.order.service.OrderShipmentJpaEntityService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class ShipmentServiceKyoungdongTrackingTest {
    @Test
    fun `customer order refresh marks a Kyoungdong shipment delivered from latest carrier scan`() {
        val candidate = ShipmentTrackingCandidateDto(
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