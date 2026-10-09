package com.buyeong.umji.api.persistence.jpa.order.repository

import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderTaxInvoiceEntity
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface PurchaseOrderTaxInvoiceRepository : JpaRepository<PurchaseOrderTaxInvoiceEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invoice from PurchaseOrderTaxInvoiceEntity invoice join fetch invoice.order where invoice.order.publicId = :orderId")
    fun findLockedByOrderPublicId(@Param("orderId") orderId: UUID): PurchaseOrderTaxInvoiceEntity?

    @EntityGraph(attributePaths = ["order", "order.account"])
    fun findAllByStatusInOrderByOrder_OrderedAtDesc(statuses: Collection<String>, pageable: Pageable): Page<PurchaseOrderTaxInvoiceEntity>
}