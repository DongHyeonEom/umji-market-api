package com.buyeong.umji.api.shipment.integration.tracking

import com.buyeong.umji.api.shipment.model.CarrierTrackingStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class OfficialTrackingHtmlAdapterTest : DescribeSpec({
    val daesin = DaesinTrackingClient()
    val chunil = ChunilTrackingClient()

    it("대신택배 조회 HTML의 완료 셀을 배송완료로 판정한다") {
        val html = "<table><tr><td>배송완료</td></tr></table>"
        daesin.parse(html, "1501602023302") shouldBe CarrierTrackingStatus.DELIVERED
    }

    it("대신택배 조회 결과 없음은 배송완료로 처리하지 않는다") {
        val html = "<p>검색하신 운송장번호로 운송된 내역이 없습니다.</p>"
        daesin.parse(html, "1501602023302") shouldBe CarrierTrackingStatus.NOT_FOUND
    }

    it("천일택배 결과 항목이 배송완료이면 배송완료로 판정한다") {
        val html = "<table><tr><td>배송/출고결과 :</td><td>배송완료</td></tr></table>"
        chunil.parse(html) shouldBe CarrierTrackingStatus.DELIVERED
    }

    it("천일택배 조회 결과가 아직 배송완료가 아니면 배송중으로 유지한다") {
        val html = "<table><tr><td>배송/출고결과 :</td><td>배송출발</td></tr></table>"
        chunil.parse(html) shouldBe CarrierTrackingStatus.IN_TRANSIT
    }

    it("택배 조회 HTML에 상태 항목이 없으면 미확인 상태로 반환한다") {
        chunil.parse("<html><body>조회 결과가 없습니다.</body></html>") shouldBe CarrierTrackingStatus.UNAVAILABLE
    }
})
