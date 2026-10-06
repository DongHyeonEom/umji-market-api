package com.buyeong.umji.api.persistence.jpa.inventory.repository

import com.buyeong.umji.api.persistence.jpa.inventory.entity.StockReservationEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface StockReservationRepository : JpaRepository<StockReservationEntity, Long> {
    fun findByReservationKey(reservationKey: UUID): StockReservationEntity?
}
