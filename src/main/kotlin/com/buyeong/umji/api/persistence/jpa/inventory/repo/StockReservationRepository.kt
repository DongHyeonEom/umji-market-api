package com.buyeong.umji.api.persistence.jpa.inventory

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StockReservationRepository : JpaRepository<StockReservationEntity, Long> {
    fun findByReservationKey(reservationKey: UUID): StockReservationEntity?
}
