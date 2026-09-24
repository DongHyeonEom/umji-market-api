package com.buyeong.umji.api.persistence.jpa.order

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface OrderNumberSequenceRepository : JpaRepository<OrderNumberSequenceEntity, LocalDate> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sequence from OrderNumberSequenceEntity sequence where sequence.orderDate = :orderDate")
    fun findLockedByOrderDate(@Param("orderDate") orderDate: LocalDate): OrderNumberSequenceEntity?
}