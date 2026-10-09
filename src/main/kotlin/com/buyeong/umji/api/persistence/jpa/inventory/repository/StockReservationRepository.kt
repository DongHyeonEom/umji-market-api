package com.buyeong.umji.api.persistence.jpa.inventory.repository

import com.buyeong.umji.api.persistence.jpa.inventory.entity.StockReservationEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StockReservationRepository : JpaRepository<StockReservationEntity, Long> {
    fun findByReservationKey(reservationKey: UUID): StockReservationEntity?
}