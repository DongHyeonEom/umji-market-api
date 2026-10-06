package com.buyeong.umji.api.persistence.jpa.order.service

import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderEntity
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderShipmentRepository
import com.buyeong.umji.api.shipment.model.ShipmentTrackingCandidate
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrderShipmentJpaEntityService(
    private val shipments: OrderShipmentRepository,
    private val accounts: AccountJpaEntityService,
    private val organizations: OrganizationJpaEntityService,
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
        val organizationId = requireNotNull(organizations.activeBuyerForAccountPublicId(customerId)?.id) {
            "계정의 활성 구매자 그룹을 찾을 수 없습니다."
        }
        return shipments.findTrackingCandidatesForOrganization(organizationId)
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
        if (status == IN_TRANSIT && shipment.order.taxInvoiceStatus == WAITING_FOR_SHIPMENT) {
            val orderDate = shipment.order.orderedAt.atZone(KST).toLocalDate()
            shipment.order.taxInvoiceWrittenDate = orderDate
            shipment.order.taxInvoiceSupplyDate = orderDate
            shipment.order.taxInvoiceStatus = READY_FOR_ISSUANCE
        }
        return shipments.saveAndFlush(shipment)
    }

    private companion object {
        const val READY_TO_SHIP = "READY_TO_SHIP"
        const val IN_TRANSIT = "IN_TRANSIT"
        const val DELIVERED = "DELIVERED"
        const val WAITING_FOR_SHIPMENT = "WAITING_FOR_SHIPMENT"
        const val READY_FOR_ISSUANCE = "READY_FOR_ISSUANCE"
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
