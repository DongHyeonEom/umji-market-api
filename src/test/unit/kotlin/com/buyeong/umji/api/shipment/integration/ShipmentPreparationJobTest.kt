package com.buyeong.umji.api.shipment.integration

import com.buyeong.umji.api.order.service.ShippingHolidayService
import com.buyeong.umji.api.shipment.service.ShipmentService
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class ShipmentPreparationJobTest : DescribeSpec({
    val shipments = mockk<ShipmentService>(relaxed = true)
    val holidays = mockk<ShippingHolidayService>()
    val job = ShipmentPreparationJob(shipments, holidays)
    beforeTest { clearMocks(shipments, holidays, answers = false, recordedCalls = true) }

    it("등록 공휴일에는 배송 준비와 재고 확정을 실행하지 않는다") {
        val holiday = LocalDate.of(2026, 12, 25)
        every { holidays.isHoliday(holiday) } returns true
        job.prepareFor(holiday)
        verify(exactly = 0) { shipments.prepareReadyOrders() }
    }

    it("평일 비공휴일에 배송 준비 배치를 실행한다") {
        val weekday = LocalDate.of(2026, 12, 24)
        every { holidays.isHoliday(weekday) } returns false
        job.prepareFor(weekday)
        verify(exactly = 1) { shipments.prepareReadyOrders() }
    }

    it("주말에는 공휴일 조회나 배송 준비를 실행하지 않는다") {
        job.prepareFor(LocalDate.of(2026, 12, 26))
        verify(exactly = 0) { shipments.prepareReadyOrders() }
    }
})