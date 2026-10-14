package com.buyeong.umji.api.persistence.jpa.order.repository

import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentPackageItemEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface OrderShipmentPackageItemRepository : JpaRepository<OrderShipmentPackageItemEntity, Long> {
    @Query("select coalesce(sum(packageItem.boxQuantity), 0) from OrderShipmentPackageItemEntity packageItem where packageItem.orderItem.publicId = :orderItemId")
    fun allocatedBoxQuantity(@Param("orderItemId") orderItemId: UUID): Long

    @Query("select coalesce(sum(packageItem.boxQuantity), 0) from OrderShipmentPackageItemEntity packageItem where packageItem.orderItem.id = :orderItemInternalId and packageItem.shipmentPackage.shipment.id <> :shipmentInternalId")
    fun allocatedOutsideShipment(@Param("orderItemInternalId") orderItemInternalId: Long, @Param("shipmentInternalId") shipmentInternalId: Long): Long
}
