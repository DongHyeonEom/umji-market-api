package com.buyeong.umji.api.shipment.application.port.`in`

import com.buyeong.umji.api.shipment.application.model.ShipmentChange
import java.util.UUID

interface ShipmentUseCase {
    fun registerTracking(orderId: UUID, carrierCode: String, trackingNumber: String, operatorId: UUID): ShipmentChange
    fun markDelivered(orderId: UUID, operatorId: UUID): ShipmentChange
    fun prepareOrder(orderId: UUID): ShipmentChange
    fun prepareReadyOrders(): Int
}