package com.buyeong.umji.api.order.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CarrierTrackingLinkTest {
    @Test
    fun `creates carrier links with the encoded tracking number`() {
        assertThat(CarrierTrackingLink.create("DAESIN", "1501602023302"))
            .isEqualTo("https://www.ds3211.co.kr/freight/internalFreightSearch.ht?billno=1501602023302")
        assertThat(CarrierTrackingLink.create("경동택배", "1234567890123"))
            .isEqualTo("https://kdexp.com/service/delivery/etc/delivery.do?barcode=1234567890123")
        assertThat(CarrierTrackingLink.create("CHUNIL", "72601701177"))
            .isEqualTo("https://www.chunil.co.kr/HTrace/HTrace.jsp?transNo=72601701177")
    }

    @Test
    fun `returns no link without a supported carrier and tracking number`() {
        assertThat(CarrierTrackingLink.create("UNKNOWN", "123")).isNull()
        assertThat(CarrierTrackingLink.create("DAESIN", null)).isNull()
        assertThat(CarrierTrackingLink.create("DAESIN", " ")).isNull()
    }
}