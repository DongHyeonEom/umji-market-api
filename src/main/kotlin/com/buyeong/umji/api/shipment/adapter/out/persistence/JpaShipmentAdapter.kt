package com.buyeong.umji.api.shipment.adapter.out.persistence

import com.buyeong.umji.api.inventory.application.port.`in`.InventoryUseCase
import com.buyeong.umji.api.persistence.jpa.order.OrderShipmentEntity
import com.buyeong.umji.api.persistence.jpa.order.OrderShipmentJpaEntityService
import com.buyeong.umji.api.shipment.application.model.ShipmentRecord
import com.buyeong.umji.api.shipment.application.port.out.ShipmentInventoryPort
import com.buyeong.umji.api.shipment.application.port.out.ShipmentStorePort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JpaShipmentAdapter(
    private val shipments: OrderShipmentJpaEntityService,
    private val inventory: InventoryUseCase,
) : ShipmentStorePort, ShipmentInventoryPort {
    override fun lock(orderId: UUID): ShipmentRecord? = shipments.findForUpdate(orderId)?.toRecord()
    override fun readyOrderIds(): List<UUID> = shipments.readyOrderIds()

    override fun update(
        record: ShipmentRecord,
        status: String,
        carrierCode: String?,
        trackingNumber: String?,
        operatorId: UUID?,
    ): ShipmentRecord {
        val entity = requireNotNull(shipments.findForUpdate(record.orderId)) { "주문 배송 정보를 찾을 수 없습니다." }
        return shipments.update(entity, status, carrierCode, trackingNumber, operatorId).toRecord()
    }

    override fun confirm(reservationKey: UUID) {
        inventory.confirm(reservationKey)
    }

    private fun OrderShipmentEntity.toRecord() = ShipmentRecord(
        orderId = requireNotNull(order.publicId),
        status = status,
        carrierCode = carrierCode,
        trackingNumber = trackingNumber,
        reservationKeys = order.items.map { it.reservationKey },
    )
}