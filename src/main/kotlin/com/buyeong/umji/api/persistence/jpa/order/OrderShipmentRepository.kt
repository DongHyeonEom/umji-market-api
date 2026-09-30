package com.buyeong.umji.api.persistence.jpa.order

import com.buyeong.umji.api.shipment.application.model.ShipmentTrackingCandidate
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface OrderShipmentRepository : JpaRepository<OrderShipmentEntity, Long> {
    @Query("select shipment.order.publicId from OrderShipmentEntity shipment where shipment.status = 'READY_TO_SHIP' order by shipment.createdAt")
    fun findReadyOrderIds(): List<UUID>

    @Query(
        "select new com.buyeong.umji.api.shipment.application.model.ShipmentTrackingCandidate(" +
            "purchaseOrder.publicId, shipment.status, shipment.carrierCode, shipment.trackingNumber) " +
            "from OrderShipmentEntity shipment join shipment.order purchaseOrder " +
            "where shipment.status = 'IN_TRANSIT' and shipment.carrierCode is not null and shipment.trackingNumber is not null " +
            "order by shipment.updatedAt",
    )
    fun findTrackingCandidates(): List<ShipmentTrackingCandidate>

    @Query(
        "select new com.buyeong.umji.api.shipment.application.model.ShipmentTrackingCandidate(" +
            "purchaseOrder.publicId, shipment.status, shipment.carrierCode, shipment.trackingNumber) " +
            "from OrderShipmentEntity shipment join shipment.order purchaseOrder join purchaseOrder.account account " +
            "where purchaseOrder.publicId = :orderId and account.publicId = :customerId",
    )
    fun findTrackingCandidateForCustomer(
        @Param("orderId") orderId: UUID,
        @Param("customerId") customerId: UUID,
    ): ShipmentTrackingCandidate?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shipment from OrderShipmentEntity shipment join fetch shipment.order purchaseOrder where purchaseOrder.publicId = :orderId")
    fun findForUpdateByOrderId(@Param("orderId") orderId: UUID): OrderShipmentEntity?
}