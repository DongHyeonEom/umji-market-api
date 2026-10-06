package com.buyeong.umji.api.persistence.jpa.order.repository

import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentEntity
import com.buyeong.umji.api.shipment.model.ShipmentTrackingCandidate
import jakarta.persistence.LockModeType
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OrderShipmentRepository : JpaRepository<OrderShipmentEntity, Long> {
    @Query("select shipment.order.publicId from OrderShipmentEntity shipment where shipment.status = 'READY_TO_SHIP' order by shipment.createdAt")
    fun findReadyOrderIds(): List<UUID>

    @Query(
        "select new com.buyeong.umji.api.shipment.model.ShipmentTrackingCandidate(" +
            "purchaseOrder.publicId, shipment.status, shipment.carrierCode, shipment.trackingNumber) " +
            "from OrderShipmentEntity shipment join shipment.order purchaseOrder " +
            "where shipment.status = 'IN_TRANSIT' and shipment.carrierCode is not null and shipment.trackingNumber is not null " +
            "and purchaseOrder.buyerGroup.id = :buyerGroupId " +
            "order by shipment.updatedAt",
    )
    fun findTrackingCandidatesForBuyerGroup(@Param("buyerGroupId") buyerGroupId: Long): List<ShipmentTrackingCandidate>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shipment from OrderShipmentEntity shipment join fetch shipment.order purchaseOrder where purchaseOrder.publicId = :orderId")
    fun findForUpdateByOrderId(@Param("orderId") orderId: UUID): OrderShipmentEntity?
}
