package com.buyeong.umji.api.persistence.jpa.order

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ShippingHolidayRepository : JpaRepository<ShippingHolidayEntity, LocalDate>