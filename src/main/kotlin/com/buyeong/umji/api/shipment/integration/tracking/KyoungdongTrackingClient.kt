package com.buyeong.umji.api.shipment.integration.tracking

import com.buyeong.umji.api.shipment.model.CarrierTrackingStatus
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.jsoup.Jsoup
import org.springframework.stereotype.Component

@Component
class KyoungdongTrackingClient(
    private val objectMapper: ObjectMapper,
) {
    fun supports(carrierCode: String): Boolean =
        carrierCode.trim().uppercase() in setOf("KDEXP", "KD", "KYUNGDONG", "경동택배")

    fun lookup(trackingNumber: String): CarrierTrackingStatus {
        val encodedNumber = URLEncoder.encode(trackingNumber, StandardCharsets.UTF_8)
        val response = Jsoup.connect("$LOOKUP_URL?barcode=$encodedNumber")
            .ignoreContentType(true)
            .header("User-Agent", USER_AGENT)
            .timeout(LOOKUP_TIMEOUT_MILLIS)
            .maxBodySize(MAX_RESPONSE_BYTES)
            .get()
            .body()
            .text()
        return parse(response)
    }

    internal fun parse(response: String): CarrierTrackingStatus {
        val root = objectMapper.readTree(response)
        if (root.path("result").asText() != SUCCESS_RESULT) return CarrierTrackingStatus.UNAVAILABLE

        val scans = root.path("data").path("scanList")
        if (!scans.isArray || scans.isEmpty) return CarrierTrackingStatus.UNAVAILABLE

        val latestStatus = scans.last().path("scanTypeNm").asText().trim()
        if (latestStatus.isEmpty()) return CarrierTrackingStatus.UNAVAILABLE

        return if (latestStatus == DELIVERED_LABEL) CarrierTrackingStatus.DELIVERED else CarrierTrackingStatus.IN_TRANSIT
    }

    private companion object {
        const val LOOKUP_URL = "https://kdexp.com/service/delivery/new/ajax_basic.do"
        const val SUCCESS_RESULT = "suc"
        const val DELIVERED_LABEL = "배송완료"
        const val LOOKUP_TIMEOUT_MILLIS = 5_000
        const val MAX_RESPONSE_BYTES = 1_000_000
        const val USER_AGENT = "Mozilla/5.0 UmjiMarketShipmentTracking/1.0"
    }
}
