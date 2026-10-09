package com.buyeong.umji.api.shipment.service

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.inventory.service.InventoryService
import com.buyeong.umji.api.notification.dto.NotificationEventType
import com.buyeong.umji.api.notification.service.NotificationEventService
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderShipmentEntity
import com.buyeong.umji.api.persistence.jpa.order.service.OrderShipmentJpaEntityService
import com.buyeong.umji.api.shipment.dto.ShipmentChangeDto
import com.buyeong.umji.api.shipment.dto.ShipmentRecordDto
import com.buyeong.umji.api.shipment.dto.ShipmentTrackingCandidateDto
import com.buyeong.umji.api.shipment.integration.tracking.OfficialCarrierTrackingGateway
import com.buyeong.umji.api.shipment.model.CarrierTrackingStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class ShipmentService(
    private val shipments: OrderShipmentJpaEntityService,
    private val inventory: InventoryService,
    private val tracking: OfficialCarrierTrackingGateway,
    private val notifications: NotificationEventService,
) {
    fun registerTracking(orderId: UUID, carrierCode: String, trackingNumber: String, operatorId: UUID): ShipmentChangeDto {
        val carrier = carrierCode.trim()
        val tracking = trackingNumber.trim()
        require(carrier.isNotEmpty() && carrier.length <= MAX_CARRIER_LENGTH) { "택배사 코드는 1~80자여야 합니다." }
        require(tracking.isNotEmpty() && tracking.length <= MAX_TRACKING_LENGTH) { "송장번호는 1~100자여야 합니다." }
        val current = shipments.findForUpdate(orderId)?.toRecord() ?: throw ItemNotFoundException("주문 배송 정보를 찾을 수 없습니다.")
        if (current.status == IN_TRANSIT && current.carrierCode == carrier && current.trackingNumber == tracking) {
            return current.toChange(false)
        }
        require(current.status == PREPARING || current.status == IN_TRANSIT) { "발송 처리 중인 주문만 송장 정보를 등록할 수 있습니다." }
        val entity = requireNotNull(shipments.findForUpdate(orderId))
        val changed = shipments.update(entity, IN_TRANSIT, carrier, tracking, operatorId).toRecord()
        notifications.record(NotificationEventType.SHIPMENT_IN_TRANSIT, orderId)
        return changed.toChange(true)
    }

    fun markDelivered(orderId: UUID, operatorId: UUID): ShipmentChangeDto {
        val current = shipments.findForUpdate(orderId)?.toRecord() ?: throw ItemNotFoundException("주문 배송 정보를 찾을 수 없습니다.")
        if (current.status == DELIVERED) return current.toChange(false)
        require(current.status == IN_TRANSIT) { "배송 중인 주문만 배송 완료 처리할 수 있습니다." }
        val entity = requireNotNull(shipments.findForUpdate(orderId))
        val delivered = shipments.update(entity, DELIVERED, current.carrierCode, current.trackingNumber, operatorId).toRecord()
        notifications.record(NotificationEventType.SHIPMENT_DELIVERED, orderId)
        return delivered.toChange(true)
    }

    fun refreshForCustomer(customerId: UUID): Int = shipments.trackingCandidatesForCustomer(customerId).count { candidate ->
        synchronize(candidate).changed
    }

    private fun synchronize(candidate: ShipmentTrackingCandidateDto): ShipmentChangeDto {
        if (candidate.status != IN_TRANSIT) return candidate.toChange(false)
        val lookup = tracking.lookup(candidate.carrierCode, candidate.trackingNumber)
        if (lookup == CarrierTrackingStatus.DELIVERED && shipments.markDeliveredIfCurrent(candidate)) {
            notifications.record(NotificationEventType.SHIPMENT_DELIVERED, candidate.orderId)
            return candidate.toChange(true, DELIVERED)
        }
        return candidate.toChange(false)
    }

    fun prepareReadyOrders(): Int {
        var prepared = 0
        shipments.readyOrderIds().forEach { orderId ->
            if (prepareOrder(orderId).changed) prepared++
        }
        return prepared
    }

    fun prepareOrder(orderId: UUID): ShipmentChangeDto {
        val entity = shipments.findForUpdate(orderId) ?: throw ItemNotFoundException("주문 배송 정보를 찾을 수 없습니다.")
        val current = entity.toRecord()
        if (current.status == PREPARING) return current.toChange(false)
        require(current.status == READY_TO_SHIP) { "배송 준비 가능한 주문 상태가 아닙니다." }
        current.reservationKeys.forEach(inventory::confirm)
        val preparing = shipments.update(entity, PREPARING, null, null, null).toRecord()
        notifications.record(NotificationEventType.SHIPMENT_PREPARING, orderId)
        return preparing.toChange(true)
    }

    private fun OrderShipmentEntity.toRecord() = com.buyeong.umji.api.shipment.dto.ShipmentRecordDto(
        orderId = requireNotNull(order.publicId),
        status = status,
        carrierCode = carrierCode,
        trackingNumber = trackingNumber,
        reservationKeys = order.items.map { it.reservationKey },
    )

    private fun com.buyeong.umji.api.shipment.dto.ShipmentRecordDto.toChange(changed: Boolean) =
        ShipmentChangeDto(orderId, status, carrierCode, trackingNumber, changed)

    private fun ShipmentTrackingCandidateDto.toChange(
        changed: Boolean,
        status: String = this.status,
    ) = ShipmentChangeDto(orderId, status, carrierCode, trackingNumber, changed)

    private companion object {
        const val READY_TO_SHIP = "READY_TO_SHIP"
        const val PREPARING = "PREPARING"
        const val IN_TRANSIT = "IN_TRANSIT"
        const val DELIVERED = "DELIVERED"
        const val MAX_CARRIER_LENGTH = 80
        const val MAX_TRACKING_LENGTH = 100
    }
}