package com.buyeong.umji.api.persistence.jpa.order.service

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.entity.ShippingHolidayEntity
import com.buyeong.umji.api.persistence.jpa.order.repository.ShippingHolidayRepository
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ShippingHolidayJpaEntityService(
    private val holidays: ShippingHolidayRepository,
    private val accounts: AccountJpaEntityService,
) {
    fun list(): List<ShippingHolidayEntity> = holidays.findAll().sortedBy { it.holidayDate }

    fun exists(date: LocalDate): Boolean = holidays.existsById(date)

    @Transactional
    fun register(date: LocalDate, description: String?, operatorId: UUID) {
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

    @Transactional
    fun remove(date: LocalDate) {
        require(holidays.existsById(date)) { "등록된 공휴일을 찾을 수 없습니다." }
        holidays.deleteById(date)
    }
}
