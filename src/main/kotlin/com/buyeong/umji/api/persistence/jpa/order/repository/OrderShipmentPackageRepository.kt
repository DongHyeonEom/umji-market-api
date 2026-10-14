package com.buyeong.umji.api.persistence.jpa.order.repository

import com.buyeong.umji.api.domain.shipment.dto.ShipmentTrackingCandidateDto
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentPackageEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface OrderShipmentPackageRepository : JpaRepository<OrderShipmentPackageEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shipmentPackage from OrderShipmentPackageEntity shipmentPackage join fetch shipmentPackage.shipment shipment join fetch shipment.order purchaseOrder where shipmentPackage.publicId = :packageId")
    fun findForUpdateByPublicId(@Param("packageId") packageId: UUID): OrderShipmentPackageEntity?

    @Query(
        "select new com.buyeong.umji.api.domain.shipment.dto.ShipmentTrackingCandidateDto(" +
            "purchaseOrder.publicId, shipmentPackage.publicId, shipmentPackage.status, shipmentPackage.carrierCode, shipmentPackage.trackingNumber) " +
            "from OrderShipmentPackageEntity shipmentPackage join shipmentPackage.shipment shipment join shipment.order purchaseOrder " +
            "where shipmentPackage.status = 'IN_TRANSIT' and purchaseOrder.organization.id = :organizationId order by shipmentPackage.updatedAt",
    )
    fun findTrackingCandidatesForOrganization(@Param("organizationId") organizationId: Long): List<ShipmentTrackingCandidateDto>

    fun findAllByShipment_Id(shipmentId: Long): List<OrderShipmentPackageEntity>
    fun findAllByShipment_Order_PublicId(orderId: UUID): List<OrderShipmentPackageEntity>

    @Query("select distinct shipmentPackage from OrderShipmentPackageEntity shipmentPackage join fetch shipmentPackage.shipment shipment join fetch shipment.order purchaseOrder left join fetch shipmentPackage.brand left join fetch shipmentPackage.items packageItem left join fetch packageItem.orderItem where shipmentPackage.brand.publicId in :brandIds or :includeAll = true order by shipmentPackage.updatedAt desc")
    fun findAllForOperation(@Param("brandIds") brandIds: Set<UUID>, @Param("includeAll") includeAll: Boolean): List<OrderShipmentPackageEntity>
}
