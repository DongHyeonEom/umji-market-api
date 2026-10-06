package com.buyeong.umji.api.shipment.integration

import com.buyeong.umji.api.shipment.service.ShipmentService
import com.buyeong.umji.api.order.service.ShippingHolidayService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

@Component
class ShipmentPreparationJob(
    private val shipments: ShipmentService,
    private val holidays: ShippingHolidayService,
) {
    @Scheduled(cron = "0 0 15 * * MON-FRI", zone = "Asia/Seoul")
    fun prepareWeekdayShipments() {
        prepareFor(LocalDate.now(KOREA_ZONE))
    }

    fun prepareFor(date: LocalDate) {
        if (date.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) && !holidays.isHoliday(date)) {
            shipments.prepareReadyOrders()
        }
    }

    private companion object {
        val KOREA_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
