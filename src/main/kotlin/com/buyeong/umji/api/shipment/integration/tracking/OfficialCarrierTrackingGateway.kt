package com.buyeong.umji.api.shipment.integration.tracking

import com.buyeong.umji.api.shipment.model.CarrierTrackingStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OfficialCarrierTrackingGateway(
    private val chunil: ChunilTrackingClient,
    private val daesin: DaesinTrackingClient,
    private val kyoungdong: KyoungdongTrackingClient,
) {
    fun lookup(carrierCode: String, trackingNumber: String): CarrierTrackingStatus {
        val normalizedCarrier = carrierCode.trim().uppercase()
        val lookup = when (normalizedCarrier) {
            "CHUNIL", "천일택배" -> chunil::lookup
            "DAESIN", "대신택배" -> daesin::lookup
            "KDEXP", "경동택배" -> kyoungdong::lookup
            else -> return CarrierTrackingStatus.UNAVAILABLE
        }
        return try {
            lookup(trackingNumber)
        } catch (exception: Exception) {
            logger.warn("Carrier tracking lookup failed for {}", carrierCode)
            CarrierTrackingStatus.UNAVAILABLE
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(OfficialCarrierTrackingGateway::class.java)
    }
}
