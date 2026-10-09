package com.buyeong.umji.api.persistence.jpa.order.repository

import com.buyeong.umji.api.persistence.jpa.order.entity.OrderPaymentEntity
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface OrderPaymentRepository : JpaRepository<OrderPaymentEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select distinct payment from OrderPaymentEntity payment join fetch payment.order purchaseOrder left join fetch purchaseOrder.items where purchaseOrder.publicId = :orderId",
    )
    fun findForUpdateByOrderId(@Param("orderId") orderId: UUID): OrderPaymentEntity?

    @Query(
        "select payment from OrderPaymentEntity payment join fetch payment.order purchaseOrder join fetch purchaseOrder.account where payment.status in :statuses",
    )
    fun findAllForOperation(@Param("statuses") statuses: Set<String>, pageable: Pageable): Page<OrderPaymentEntity>

    fun findAllByStatus(status: String, pageable: Pageable): Page<OrderPaymentEntity>
}