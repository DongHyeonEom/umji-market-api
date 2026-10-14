package com.buyeong.umji.api.persistence.jpa.order.repository

import com.buyeong.umji.api.domain.operation.order.dto.OperationOrderSearchRowDto
import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderEntity
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface PurchaseOrderRepository : JpaRepository<PurchaseOrderEntity, Long> {
    @Query(
        value = """
            select new com.buyeong.umji.api.domain.operation.order.dto.OperationOrderSearchRowDto(
                purchaseOrder.publicId, purchaseOrder.orderNumber, purchaseOrder.status, purchaseOrder.orderedAt,
                organization.publicId, organization.displayName, account.name, account.phone, purchaseOrder.totalAmount,
                payment.status, shipment.status, case when invoice.id is null then false else true end, invoice.status
            )
            from PurchaseOrderEntity purchaseOrder
            join purchaseOrder.account account
            join purchaseOrder.organization organization
            join purchaseOrder.payment payment
            join purchaseOrder.shipment shipment
            left join purchaseOrder.taxInvoice invoice
            where purchaseOrder.orderedAt >= :from and purchaseOrder.orderedAt < :until
                and (:organizationName is null or lower(organization.displayName) like concat('%', :organizationName, '%'))
                and (:ordererName is null or lower(account.name) like concat('%', :ordererName, '%'))
                and (:paymentStatus is null or payment.status = :paymentStatus)
                and (:shipmentStatus is null or shipment.status = :shipmentStatus)
                and (:orderNumber is null or lower(purchaseOrder.orderNumber) like concat('%', :orderNumber, '%'))
            order by purchaseOrder.orderedAt desc, purchaseOrder.id desc
        """,
        countQuery = """
            select count(purchaseOrder)
            from PurchaseOrderEntity purchaseOrder
            join purchaseOrder.account account
            join purchaseOrder.organization organization
            join purchaseOrder.payment payment
            join purchaseOrder.shipment shipment
            where purchaseOrder.orderedAt >= :from and purchaseOrder.orderedAt < :until
                and (:organizationName is null or lower(organization.displayName) like concat('%', :organizationName, '%'))
                and (:ordererName is null or lower(account.name) like concat('%', :ordererName, '%'))
                and (:paymentStatus is null or payment.status = :paymentStatus)
                and (:shipmentStatus is null or shipment.status = :shipmentStatus)
                and (:orderNumber is null or lower(purchaseOrder.orderNumber) like concat('%', :orderNumber, '%'))
        """,
    )
    fun searchForOperation(
        @Param("from") from: java.time.Instant,
        @Param("until") until: java.time.Instant,
        @Param("organizationName") organizationName: String?,
        @Param("ordererName") ordererName: String?,
        @Param("paymentStatus") paymentStatus: String?,
        @Param("shipmentStatus") shipmentStatus: String?,
        @Param("orderNumber") orderNumber: String?,
        pageable: Pageable,
    ): Page<OperationOrderSearchRowDto>

    @EntityGraph(attributePaths = ["account", "organization", "payment", "shipment", "taxInvoice", "items"])
    fun findByPublicId(publicId: UUID): PurchaseOrderEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select distinct purchaseOrder from PurchaseOrderEntity purchaseOrder join fetch purchaseOrder.account join fetch purchaseOrder.payment join fetch purchaseOrder.shipment left join fetch purchaseOrder.items item left join fetch purchaseOrder.taxInvoice where purchaseOrder.publicId = :publicId",
    )
    fun findForCancellation(@Param("publicId") publicId: UUID): PurchaseOrderEntity?

    @Query(
        "select distinct purchaseOrder from PurchaseOrderEntity purchaseOrder join fetch purchaseOrder.account left join fetch purchaseOrder.items item left join fetch purchaseOrder.taxInvoice where purchaseOrder.publicId = :publicId and purchaseOrder.organization.id = :organizationId",
    )
    fun findWithItemsByPublicIdAndOrganizationId(
        @Param("publicId") publicId: UUID,
        @Param("organizationId") organizationId: Long,
    ): PurchaseOrderEntity?

    @EntityGraph(attributePaths = ["account", "payment", "shipment", "taxInvoice"])
    fun findAllByOrganization_Id(organizationId: Long, pageable: Pageable): Page<PurchaseOrderEntity>
}