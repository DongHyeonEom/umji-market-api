package com.buyeong.umji.api.persistence.jpa.order.repository

import com.buyeong.umji.api.domain.shipment.dto.ShipmentTrackingCandidateDto
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentPackageEntity
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface OrderShipmentPackageRepository : JpaRepository<OrderShipmentPackageEntity, Long> {
    @Query(
        "select count(shipmentPackage) as packageCount, coalesce(sum(shipmentPackage.actualShippingCost), 0) as actualCostTotal, " +
            "coalesce(sum(case when shipmentPackage.actualShippingCost is null then 1 else 0 end), 0) as missingCostCount " +
            "from OrderShipmentPackageEntity shipmentPackage where shipmentPackage.brand.publicId = :brandId " +
            "and shipmentPackage.createdAt >= :from and shipmentPackage.createdAt < :until",
    )
    fun aggregateShippingCosts(
        @Param("brandId") brandId: UUID,
        @Param("from") from: java.time.Instant,
        @Param("until") until: java.time.Instant,
    ): ShipmentCostSummaryProjection

    @Query(
        "select shipmentPackage.carrierCode as carrierCode, count(shipmentPackage) as packageCount, " +
            "coalesce(sum(shipmentPackage.actualShippingCost), 0) as actualCostTotal, " +
            "coalesce(sum(case when shipmentPackage.actualShippingCost is null then 1 else 0 end), 0) as missingCostCount " +
            "from OrderShipmentPackageEntity shipmentPackage where shipmentPackage.brand.publicId = :brandId " +
            "and shipmentPackage.createdAt >= :from and shipmentPackage.createdAt < :until " +
            "group by shipmentPackage.carrierCode order by shipmentPackage.carrierCode",
    )
    fun aggregateShippingCostsByCarrier(
        @Param("brandId") brandId: UUID,
        @Param("from") from: java.time.Instant,
        @Param("until") until: java.time.Instant,
    ): List<ShipmentCarrierCostProjection>

    @EntityGraph(attributePaths = ["shipment", "shipment.order"])
    @Query(
        "select shipmentPackage from OrderShipmentPackageEntity shipmentPackage " +
            "where shipmentPackage.brand.publicId = :brandId and shipmentPackage.createdAt >= :from " +
            "and shipmentPackage.createdAt < :until order by shipmentPackage.createdAt desc, shipmentPackage.id desc",
    )
    fun findAllForSettlement(
        @Param("brandId") brandId: UUID,
        @Param("from") from: java.time.Instant,
        @Param("until") until: java.time.Instant,
        pageable: Pageable,
    ): Page<OrderShipmentPackageEntity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "select shipmentPackage from OrderShipmentPackageEntity shipmentPackage join fetch shipmentPackage.shipment shipment join fetch shipment.order purchaseOrder where shipmentPackage.publicId = :packageId"
    )
    fun findForUpdateByPublicId(@Param("packageId") packageId: UUID): OrderShipmentPackageEntity?

    @Query(
        "select new com.buyeong.umji.api.domain.shipment.dto.ShipmentTrackingCandidateDto(" +
            "purchaseOrder.publicId, shipmentPackage.publicId, shipmentPackage.status, shipmentPackage.carrierCode, shipmentPackage.trackingNumber) " +
            "from OrderShipmentPackageEntity shipmentPackage join shipmentPackage.shipment shipment join shipment.order purchaseOrder " +
            "where shipmentPackage.status = 'IN_TRANSIT' and purchaseOrder.organization.id = :organizationId order by shipmentPackage.updatedAt",
    )
    fun findTrackingCandidatesForOrganization(@Param("organizationId") organizationId: Long): List<ShipmentTrackingCandidateDto>

    fun findAllByShipment_Id(shipmentId: Long): List<OrderShipmentPackageEntity>

    @EntityGraph(attributePaths = ["brand", "items", "items.orderItem"])
    fun findAllByShipment_Order_PublicId(orderId: UUID): List<OrderShipmentPackageEntity>

    @EntityGraph(attributePaths = ["shipment", "shipment.order", "items", "items.orderItem"])
    fun findAllByShipment_Order_OrderedAtGreaterThanEqualAndShipment_Order_OrderedAtLessThan(from: java.time.Instant, until: java.time.Instant): List<OrderShipmentPackageEntity>

    @Query(
        "select distinct shipmentPackage from OrderShipmentPackageEntity shipmentPackage join fetch shipmentPackage.shipment shipment join fetch shipment.order purchaseOrder left join fetch shipmentPackage.brand left join fetch shipmentPackage.items packageItem left join fetch packageItem.orderItem where shipmentPackage.brand.publicId in :brandIds or :includeAll = true order by shipmentPackage.updatedAt desc"
    )
    fun findAllForOperation(@Param("brandIds") brandIds: Set<UUID>, @Param("includeAll") includeAll: Boolean): List<OrderShipmentPackageEntity>
}
