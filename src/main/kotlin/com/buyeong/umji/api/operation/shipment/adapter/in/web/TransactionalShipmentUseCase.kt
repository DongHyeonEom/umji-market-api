package com.buyeong.umji.api.operation.shipment.adapter.`in`.web

import com.buyeong.umji.api.shipment.application.model.ShipmentChange
import com.buyeong.umji.api.shipment.application.port.`in`.ShipmentUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TransactionalShipmentUseCase(private val shipments: ShipmentUseCase) {
    @Transactional
    fun beginDispatch(orderId: UUID, operatorId: UUID): ShipmentChange = shipments.beginDispatch(orderId, operatorId)

    @Transactional
    fun registerTracking(orderId: UUID, carrierCode: String, trackingNumber: String, operatorId: UUID): ShipmentChange =
        shipments.registerTracking(orderId, carrierCode, trackingNumber, operatorId)
}