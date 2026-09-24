package com.buyeong.umji.api.persistence.jpa.order

import org.springframework.data.jpa.repository.JpaRepository

interface OrderStatusHistoryRepository : JpaRepository<OrderStatusHistoryEntity, Long>