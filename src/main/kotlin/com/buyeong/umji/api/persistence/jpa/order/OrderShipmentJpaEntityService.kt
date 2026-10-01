package com.buyeong.umji.api.persistence.jpa.order

import com.buyeong.umji.api.persistence.jpa.account.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupJpaEntityService
import com.buyeong.umji.api.shipment.application.model.ShipmentTrackingCandidate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OrderShipmentJpaEntityService(
    private val shipments: OrderShipmentRepository,
    private val accounts: AccountJpaEntityService,
    private val buyerGroups: BuyerGroupJpaEntityService,
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

    fun readyOrderIds(): List<UUID> = shipments.findReadyOrderIds()

    fun trackingCandidatesForCustomer(customerId: UUID): List<ShipmentTrackingCandidate> {
        val buyerGroupId = requireNotNull(buyerGroups.activeForAccountPublicId(customerId)?.id) {
            "계정의 활성 구매자 그룹을 찾을 수 없습니다."
        }
        return shipments.findTrackingCandidatesForBuyerGroup(buyerGroupId)
    }

    @Transactional
    fun markDeliveredIfCurrent(candidate: ShipmentTrackingCandidate): Boolean {
        val current = shipments.findForUpdateByOrderId(candidate.orderId) ?: return false
        if (current.status != IN_TRANSIT || current.carrierCode != candidate.carrierCode || current.trackingNumber != candidate.trackingNumber) {
            return false
        }
        update(current, DELIVERED, current.carrierCode, current.trackingNumber, null)
        return true
    }

    @Transactional
    fun update(
        shipment: OrderShipmentEntity,
        status: String,
        carrierCode: String?,
        trackingNumber: String?,
        operatorId: UUID?,
    ): OrderShipmentEntity {
        shipment.status = status
        shipment.carrierCode = carrierCode
        shipment.trackingNumber = trackingNumber
        shipment.processedBy = operatorId?.let { accounts.findByPublicId(it) ?: error("처리자 계정을 찾을 수 없습니다.") }
        shipment.updatedAt = Instant.now()
        return shipments.saveAndFlush(shipment)
    }

    private companion object {
        const val READY_TO_SHIP = "READY_TO_SHIP"
        const val IN_TRANSIT = "IN_TRANSIT"
        const val DELIVERED = "DELIVERED"
    }
}