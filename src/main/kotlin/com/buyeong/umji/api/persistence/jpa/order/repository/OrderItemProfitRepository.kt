package com.buyeong.umji.api.persistence.jpa.order.repository

import com.buyeong.umji.api.persistence.jpa.order.entity.OrderItemEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface OrderItemProfitRepository : JpaRepository<OrderItemEntity, Long> {
    @Query("select i from OrderItemEntity i join fetch i.order o join fetch i.sku s join fetch s.product p left join fetch p.brand where o.orderedAt >= :from and o.orderedAt < :until and o.status <> 'CANCELLED' order by o.orderedAt, i.id")
    fun findForProfit(@Param("from") from: Instant, @Param("until") until: Instant): List<OrderItemEntity>
}
