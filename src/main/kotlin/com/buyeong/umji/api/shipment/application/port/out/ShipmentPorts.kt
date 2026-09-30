package com.buyeong.umji.api.shipment.application.port.out

import com.buyeong.umji.api.shipment.application.model.ShipmentRecord
import com.buyeong.umji.api.shipment.application.model.ShipmentTrackingCandidate
import java.util.UUID

interface ShipmentStorePort {
    fun lock(orderId: UUID): ShipmentRecord?
    fun readyOrderIds(): List<UUID>
    fun trackingCandidatesForCustomer(customerId: UUID): List<ShipmentTrackingCandidate>
    fun markDeliveredIfCurrent(candidate: ShipmentTrackingCandidate): Boolean
    fun update(record: ShipmentRecord, status: String, carrierCode: String?, trackingNumber: String?, operatorId: UUID?): ShipmentRecord
}

interface ShipmentTrackingPort {
    fun lookup(carrierCode: String, trackingNumber: String): com.buyeong.umji.api.shipment.application.model.CarrierTrackingStatus
}

interface ShipmentInventoryPort {
    fun confirm(reservationKey: UUID)
}