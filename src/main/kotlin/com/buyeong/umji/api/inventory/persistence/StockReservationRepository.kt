package com.buyeong.umji.api.inventory.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StockReservationRepository : JpaRepository<StockReservationEntity, Long> {
    fun findByReservationKey(reservationKey: UUID): StockReservationEntity?
}
