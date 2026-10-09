package com.buyeong.umji.api.domain.order.service

import com.buyeong.umji.api.domain.order.dto.ShippingHolidayDto
import com.buyeong.umji.api.persistence.jpa.order.service.ShippingHolidayJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ShippingHolidayService(private val holidays: ShippingHolidayJpaEntityService) {
    fun list() = holidays.list().map { ShippingHolidayDto(it.holidayDate, it.description) }
    fun isHoliday(date: LocalDate) = holidays.exists(date)

    @Transactional
    fun register(date: LocalDate, description: String?, operatorId: UUID) = holidays.register(date, description, operatorId)

    @Transactional
    fun remove(date: LocalDate) = holidays.remove(date)
}