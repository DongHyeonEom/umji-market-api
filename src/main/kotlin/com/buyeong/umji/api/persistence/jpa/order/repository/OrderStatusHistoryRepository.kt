package com.buyeong.umji.api.persistence.jpa.order.repository

import com.buyeong.umji.api.persistence.jpa.order.entity.OrderStatusHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface OrderStatusHistoryRepository : JpaRepository<OrderStatusHistoryEntity, Long>