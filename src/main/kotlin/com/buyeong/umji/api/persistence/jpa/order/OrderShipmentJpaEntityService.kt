package com.buyeong.umji.api.persistence.jpa.order

import com.buyeong.umji.api.persistence.jpa.account.AccountJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OrderShipmentJpaEntityService(
    private val shipments: OrderShipmentRepository,
    private val accounts: AccountJpaEntityService,
) {
    @Transactional
    fun initialize(order: PurchaseOrderEntity): OrderShipmentEntity = shipments.saveAndFlush(
        OrderShipmentEntity().apply {
            this.order = order
            status = READY_TO_SHIP
            createdAt = order.orderedAt
            updatedAt = order.orderedAt
        },
    )

    @Transactional
    fun findForUpdate(orderId: UUID): OrderShipmentEntity? = shipments.findForUpdateByOrderId(orderId)

    @Transactional
    fun update(
        shipment: OrderShipmentEntity,
        status: String,
        carrierCode: String?,
        trackingNumber: String?,
        operatorId: UUID,
    ): OrderShipmentEntity {
        shipment.status = status
        shipment.carrierCode = carrierCode
        shipment.trackingNumber = trackingNumber
        shipment.processedBy = accounts.findByPublicId(operatorId) ?: error("처리자 계정을 찾을 수 없습니다.")
        shipment.updatedAt = Instant.now()
        return shipments.saveAndFlush(shipment)
    }

    private companion object {
        const val READY_TO_SHIP = "READY_TO_SHIP"
    }
}