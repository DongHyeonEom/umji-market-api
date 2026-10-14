package com.buyeong.umji.api.persistence.jpa.inventory.repository

import com.buyeong.umji.api.persistence.jpa.inventory.entity.StockReservationLotAllocationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface StockReservationLotAllocationRepository : JpaRepository<StockReservationLotAllocationEntity, Long> {
    fun findAllByReservation_IdOrderById(reservationId: Long): List<StockReservationLotAllocationEntity>
}
