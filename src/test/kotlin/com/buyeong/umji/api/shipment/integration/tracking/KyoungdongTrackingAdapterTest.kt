package com.buyeong.umji.api.shipment.integration.tracking

import com.buyeong.umji.api.shipment.model.CarrierTrackingStatus
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KyoungdongTrackingClientTest {
    private val adapter = KyoungdongTrackingClient(jacksonObjectMapper())

    @Test
    fun `supports Kyoungdong carrier names`() {
        assertThat(adapter.supports("KDEXP")).isTrue()
        assertThat(adapter.supports("KD")).isTrue()
        assertThat(adapter.supports("경동택배")).isTrue()
    }

    @Test
    fun `maps the latest completed scan to delivered`() {
        val response = """
            {
              "result": "suc",
              "data": {
                "scanList": [
                  { "scanTypeNm": "발송" },
                  { "scanTypeNm": "도착" },
                  { "scanTypeNm": "배송중" },
                  { "scanTypeNm": "배송완료" }
                ]
              }
            }
        """.trimIndent()

        assertThat(adapter.parse(response)).isEqualTo(CarrierTrackingStatus.DELIVERED)
    }

    @Test
    fun `keeps the shipment in transit when its latest scan is not completed`() {
        val response = """
            { "result": "suc", "data": { "scanList": [{ "scanTypeNm": "터미널입고" }] } }
        """.trimIndent()

        assertThat(adapter.parse(response)).isEqualTo(CarrierTrackingStatus.IN_TRANSIT)
    }

    @Test
    fun `returns unavailable when the tracking number has no result`() {
        val response = """{ "result": "fail" }"""

        assertThat(adapter.parse(response)).isEqualTo(CarrierTrackingStatus.UNAVAILABLE)
    }

    @Test
    fun `returns unavailable when successful response has no scan history`() {
        val response = """{ "result": "suc", "data": { "scanList": [] } }"""

        assertThat(adapter.parse(response)).isEqualTo(CarrierTrackingStatus.UNAVAILABLE)
    }
}
