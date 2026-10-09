package com.buyeong.umji.api.persistence.jpa.order.repository

import com.buyeong.umji.api.persistence.jpa.order.entity.OrderCancellationHistoryEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface OrderCancellationHistoryRepository : JpaRepository<OrderCancellationHistoryEntity, Long> {
    fun existsByOrderPublicIdAndStatus(orderId: UUID, status: String): Boolean

    @Query(
        "select history from OrderCancellationHistoryEntity history join fetch history.order purchaseOrder join fetch purchaseOrder.account where history.status = :status order by history.requestedAt",
    )
    fun findQueue(@Param("status") status: String, pageable: Pageable): Page<OrderCancellationHistoryEntity>

    @Query(
        "select history from OrderCancellationHistoryEntity history join fetch history.order purchaseOrder where purchaseOrder.publicId = :orderId and history.status = 'PENDING' order by history.requestedAt desc",
    )
    fun findPending(@Param("orderId") orderId: UUID): List<OrderCancellationHistoryEntity>

    @Query("select history.status from OrderCancellationHistoryEntity history where history.order.publicId = :orderId order by history.requestedAt desc")
    fun findLatestStatus(@Param("orderId") orderId: UUID, pageable: Pageable): List<String>
}