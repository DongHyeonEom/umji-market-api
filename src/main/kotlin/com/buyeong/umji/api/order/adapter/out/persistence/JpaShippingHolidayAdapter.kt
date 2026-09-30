package com.buyeong.umji.api.order.adapter.out.persistence

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.order.application.model.ShippingHoliday
import com.buyeong.umji.api.order.application.port.out.ShippingHolidayPort
import com.buyeong.umji.api.persistence.jpa.account.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.ShippingHolidayEntity
import com.buyeong.umji.api.persistence.jpa.order.ShippingHolidayRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Component
class JpaShippingHolidayAdapter(private val holidays: ShippingHolidayRepository, private val accounts: AccountJpaEntityService) : ShippingHolidayPort {
    @Transactional(readOnly = true)
    override fun list() = holidays.findAll().sortedBy { it.holidayDate }.map { ShippingHoliday(it.holidayDate, it.description) }

    @Transactional(readOnly = true)
    override fun isHoliday(date: LocalDate) = holidays.existsById(date)

    @Transactional override fun register(date: LocalDate, description: String?, operatorId: UUID) {
        require(!holidays.existsById(date)) { "이미 등록된 공휴일입니다." }
        val operator = accounts.findByPublicId(operatorId) ?: throw ItemNotFoundException("운영자 계정을 찾을 수 없습니다.")
        holidays.saveAndFlush(
            ShippingHolidayEntity().apply {
                holidayDate = date
                this.description = description?.trim()?.ifBlank { null }
                creator = operator
            },
        )
    }

    @Transactional override fun remove(date: LocalDate) {
        require(holidays.existsById(date)) { "등록된 공휴일을 찾을 수 없습니다." }
        holidays.deleteById(date)
    }
}