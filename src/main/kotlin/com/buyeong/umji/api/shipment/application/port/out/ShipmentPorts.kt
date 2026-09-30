package com.buyeong.umji.api.shipment.application.port.out

import com.buyeong.umji.api.shipment.application.model.ShipmentRecord
import java.util.UUID

interface ShipmentStorePort {
    fun lock(orderId: UUID): ShipmentRecord?
    fun readyOrderIds(): List<UUID>
    fun update(record: ShipmentRecord, status: String, carrierCode: String?, trackingNumber: String?, operatorId: UUID?): ShipmentRecord
}

interface ShipmentInventoryPort {
    fun confirm(reservationKey: UUID)
}