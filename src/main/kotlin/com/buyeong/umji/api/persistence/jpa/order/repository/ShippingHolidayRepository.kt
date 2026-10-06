package com.buyeong.umji.api.persistence.jpa.order.repository

import com.buyeong.umji.api.persistence.jpa.order.entity.ShippingHolidayEntity
import java.time.LocalDate
import org.springframework.data.jpa.repository.JpaRepository

interface ShippingHolidayRepository : JpaRepository<ShippingHolidayEntity, LocalDate>
