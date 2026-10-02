package com.buyeong.umji.api.shipment.application

import com.buyeong.umji.api.shipment.adapter.out.tracking.KyoungdongTrackingAdapter
import com.buyeong.umji.api.shipment.application.model.CarrierTrackingStatus
import com.buyeong.umji.api.shipment.application.model.ShipmentRecord
import com.buyeong.umji.api.shipment.application.model.ShipmentTrackingCandidate
import com.buyeong.umji.api.shipment.application.port.out.ShipmentInventoryPort
import com.buyeong.umji.api.shipment.application.port.out.ShipmentStorePort
import com.buyeong.umji.api.shipment.application.port.out.ShipmentTrackingPort
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
        val expectedCandidate = candidate
        var markedDelivered = false
        val shipments = object : ShipmentStorePort {
            override fun lock(orderId: UUID): ShipmentRecord? = null

            override fun readyOrderIds(): List<UUID> = emptyList()

            override fun trackingCandidatesForCustomer(customerId: UUID): List<ShipmentTrackingCandidate> = listOf(candidate)

            override fun markDeliveredIfCurrent(candidate: ShipmentTrackingCandidate): Boolean {
                markedDelivered = candidate == expectedCandidate
                return markedDelivered
            }

            override fun update(
                record: ShipmentRecord,
                status: String,
                carrierCode: String?,
                trackingNumber: String?,
                operatorId: UUID?,
            ): ShipmentRecord = error("Not used by customer refresh")
        }
        val adapter = KyoungdongTrackingAdapter(jacksonObjectMapper())
        val tracking = object : ShipmentTrackingPort {
            override fun lookup(carrierCode: String, trackingNumber: String): CarrierTrackingStatus =
                adapter.parse(
                    """{"result":"suc","data":{"scanList":[{"scanTypeNm":"발송"},{"scanTypeNm":"도착"},{"scanTypeNm":"배송중"},{"scanTypeNm":"배송완료"}]}}""",
                )
        }
        val service = ShipmentService(
            shipments = shipments,
            inventory = object : ShipmentInventoryPort {
                override fun confirm(reservationKey: UUID) = error("Not used by customer refresh")
            },
            tracking = tracking,
        )

        assertThat(service.refreshForCustomer(UUID.randomUUID())).isEqualTo(1)
        assertThat(markedDelivered).isTrue()
    }
}