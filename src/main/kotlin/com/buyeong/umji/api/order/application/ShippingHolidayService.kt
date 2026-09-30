package com.buyeong.umji.api.order.application

import com.buyeong.umji.api.order.application.port.`in`.ShippingHolidayUseCase
import com.buyeong.umji.api.order.application.port.out.ShippingHolidayPort
import java.time.LocalDate
import java.util.UUID

class ShippingHolidayService(private val holidays: ShippingHolidayPort) : ShippingHolidayUseCase {
    override fun list() = holidays.list()
    override fun isHoliday(date: LocalDate) = holidays.isHoliday(date)
    override fun register(date: LocalDate, description: String?, operatorId: UUID) = holidays.register(date, description, operatorId)
    override fun remove(date: LocalDate) = holidays.remove(date)
}