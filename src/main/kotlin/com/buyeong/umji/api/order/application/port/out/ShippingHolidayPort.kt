package com.buyeong.umji.api.order.application.port.out

import com.buyeong.umji.api.order.application.model.ShippingHoliday
import java.time.LocalDate
import java.util.UUID

interface ShippingHolidayPort {
    fun list(): List<ShippingHoliday>
    fun isHoliday(date: LocalDate): Boolean
    fun register(date: LocalDate, description: String?, operatorId: UUID)
    fun remove(date: LocalDate)
}