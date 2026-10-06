package com.buyeong.umji.api.persistence.jpa.order.repository

import com.buyeong.umji.api.persistence.jpa.order.entity.OrderNumberSequenceEntity
import jakarta.persistence.LockModeType
import java.time.LocalDate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OrderNumberSequenceRepository : JpaRepository<OrderNumberSequenceEntity, LocalDate> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sequence from OrderNumberSequenceEntity sequence where sequence.orderDate = :orderDate")
    fun findLockedByOrderDate(@Param("orderDate") orderDate: LocalDate): OrderNumberSequenceEntity?
}
