package com.buyeong.umji.api.order.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface OrderStatusHistoryRepository : JpaRepository<OrderStatusHistoryEntity, Long>
