package com.buyeong.umji.api.persistence.jpa.order.repository

import com.buyeong.umji.api.persistence.jpa.order.entity.ShippingHolidayEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ShippingHolidayRepository : JpaRepository<ShippingHolidayEntity, LocalDate>