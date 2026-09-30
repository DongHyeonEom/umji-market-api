package com.buyeong.umji.api.shipment.adapter.out.tracking

import com.buyeong.umji.api.shipment.application.model.CarrierTrackingStatus
import com.buyeong.umji.api.shipment.application.port.out.ShipmentTrackingPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OfficialCarrierTrackingGateway(
    private val adapters: List<OfficialCarrierTrackingAdapter>,
) : ShipmentTrackingPort {
    override fun lookup(carrierCode: String, trackingNumber: String): CarrierTrackingStatus {
        val adapter = adapters.firstOrNull { it.supports(carrierCode) } ?: return CarrierTrackingStatus.UNAVAILABLE
        return try {
            adapter.lookup(trackingNumber)
        } catch (exception: Exception) {
            logger.warn("Carrier tracking lookup failed for {}", carrierCode)
            CarrierTrackingStatus.UNAVAILABLE
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(OfficialCarrierTrackingGateway::class.java)
    }
}