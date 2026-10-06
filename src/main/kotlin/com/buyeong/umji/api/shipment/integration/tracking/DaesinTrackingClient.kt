package com.buyeong.umji.api.shipment.integration.tracking

import com.buyeong.umji.api.shipment.model.CarrierTrackingStatus
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.jsoup.Jsoup
import org.springframework.stereotype.Component

@Component
class DaesinTrackingClient {
        fun lookup(trackingNumber: String): CarrierTrackingStatus {
        val encodedNumber = URLEncoder.encode(trackingNumber, StandardCharsets.UTF_8)
        val html = Jsoup.connect("https://www.ds3211.co.kr/freight/internalFreightSearch.ht?billno=$encodedNumber")
            .timeout(LOOKUP_TIMEOUT_MILLIS)
            .maxBodySize(MAX_RESPONSE_BYTES)
            .get()
            .html()
        return parse(html, trackingNumber)
    }

    internal fun parse(html: String, trackingNumber: String): CarrierTrackingStatus {
        val document = Jsoup.parse(html)
        val text = document.text()
        if (text.contains("운송된 내역이 없습니다") || text.contains("조회 내역이 없습니다")) {
            return CarrierTrackingStatus.NOT_FOUND
        }
        if (document.select("td").any { it.text().trim() == DELIVERED_LABEL }) {
            return CarrierTrackingStatus.DELIVERED
        }
        return if (text.contains(trackingNumber)) CarrierTrackingStatus.IN_TRANSIT else CarrierTrackingStatus.UNAVAILABLE
    }

    private companion object {
        const val DELIVERED_LABEL = "배송완료"
        const val LOOKUP_TIMEOUT_MILLIS = 5_000
        const val MAX_RESPONSE_BYTES = 1_000_000
    }
}
