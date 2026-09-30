package com.buyeong.umji.api.operation.shipment.adapter.`in`.web

import com.buyeong.umji.api.shipment.application.model.ShipmentChange
import com.buyeong.umji.api.shipment.application.port.`in`.ShipmentUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TransactionalShipmentUseCase(private val shipments: ShipmentUseCase) {
    @Transactional
    fun registerTracking(orderId: UUID, carrierCode: String, trackingNumber: String, operatorId: UUID): ShipmentChange =
        shipments.registerTracking(orderId, carrierCode, trackingNumber, operatorId)

    @Transactional
    fun markDelivered(orderId: UUID, operatorId: UUID): ShipmentChange = shipments.markDelivered(orderId, operatorId)

    fun refreshForCustomer(customerId: UUID): Int = shipments.refreshForCustomer(customerId)

    @Transactional
    fun prepareReadyOrders(): Int = shipments.prepareReadyOrders()

    @Transactional
    fun prepareOrder(orderId: UUID): ShipmentChange = shipments.prepareOrder(orderId)
}