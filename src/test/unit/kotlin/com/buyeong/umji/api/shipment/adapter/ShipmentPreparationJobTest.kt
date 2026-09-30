package com.buyeong.umji.api.shipment.adapter

import com.buyeong.umji.api.operation.shipment.adapter.`in`.web.TransactionalShipmentUseCase
import com.buyeong.umji.api.order.application.port.`in`.ShippingHolidayUseCase
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class ShipmentPreparationJobTest : DescribeSpec({
    val shipments = mockk<TransactionalShipmentUseCase>(relaxed = true)
    val holidays = mockk<ShippingHolidayUseCase>()
    val job = ShipmentPreparationJob(shipments, holidays)
    val trackingJob = ShipmentTrackingJob(shipments)

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

    it("배송 추적 작업은 배송중 주문 동기화를 호출한다") {
        trackingJob.synchronizeInTransitOrders()
        verify(exactly = 1) { shipments.synchronizeTrackingStatus() }
    }
})