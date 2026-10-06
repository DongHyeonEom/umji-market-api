package com.buyeong.umji.api.persistence.jpa.order.repository

import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderEntity
import jakarta.persistence.LockModeType
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PurchaseOrderRepository : JpaRepository<PurchaseOrderEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select distinct purchaseOrder from PurchaseOrderEntity purchaseOrder join fetch purchaseOrder.account join fetch purchaseOrder.payment join fetch purchaseOrder.shipment left join fetch purchaseOrder.items item where purchaseOrder.publicId = :publicId",
    )
    fun findForCancellation(@Param("publicId") publicId: UUID): PurchaseOrderEntity?

    @Query(
        "select distinct purchaseOrder from PurchaseOrderEntity purchaseOrder join fetch purchaseOrder.account left join fetch purchaseOrder.items item where purchaseOrder.publicId = :publicId and purchaseOrder.organization.id = :organizationId",
    )
    fun findWithItemsByPublicIdAndOrganizationId(
        @Param("publicId") publicId: UUID,
        @Param("organizationId") organizationId: Long,
    ): PurchaseOrderEntity?

    @EntityGraph(attributePaths = ["account", "payment", "shipment"])
    fun findAllByOrganization_Id(organizationId: Long, pageable: Pageable): Page<PurchaseOrderEntity>
}
