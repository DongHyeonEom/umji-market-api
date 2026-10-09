package com.buyeong.umji.api.domain.shipment.integration.tracking

import com.buyeong.umji.api.domain.shipment.model.CarrierTrackingStatus
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class ChunilTrackingClient {
    fun lookup(trackingNumber: String): CarrierTrackingStatus {
        val encodedNumber = URLEncoder.encode(trackingNumber, StandardCharsets.UTF_8)
        val html = Jsoup.connect("https://www.chunil.co.kr/HTrace/HTrace.jsp?transNo=$encodedNumber")
            .timeout(LOOKUP_TIMEOUT_MILLIS)
            .maxBodySize(MAX_RESPONSE_BYTES)
            .get()
            .html()
        return parse(html)
    }

    internal fun parse(html: String): CarrierTrackingStatus {
        val document = Jsoup.parse(html)
        val result = document.select("tr").firstNotNullOfOrNull { row ->
            val cells = row.select("td")
            if (cells.any { it.text().contains(RESULT_LABEL) }) cells.lastOrNull()?.text()?.trim() else null
        } ?: return CarrierTrackingStatus.UNAVAILABLE

        return if (result == DELIVERED_LABEL) CarrierTrackingStatus.DELIVERED else CarrierTrackingStatus.IN_TRANSIT
    }

    private companion object {
        const val RESULT_LABEL = "배송/출고결과"
        const val DELIVERED_LABEL = "배송완료"
        const val LOOKUP_TIMEOUT_MILLIS = 5_000
        const val MAX_RESPONSE_BYTES = 1_000_000
    }
}