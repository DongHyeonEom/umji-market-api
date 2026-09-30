package com.buyeong.umji.api.shipment.application

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.shipment.application.model.CarrierTrackingStatus
import com.buyeong.umji.api.shipment.application.model.ShipmentChange
import com.buyeong.umji.api.shipment.application.model.ShipmentTrackingCandidate
import com.buyeong.umji.api.shipment.application.port.`in`.ShipmentUseCase
import com.buyeong.umji.api.shipment.application.port.out.ShipmentInventoryPort
import com.buyeong.umji.api.shipment.application.port.out.ShipmentStorePort
import com.buyeong.umji.api.shipment.application.port.out.ShipmentTrackingPort
import java.util.UUID

class ShipmentService(
    private val shipments: ShipmentStorePort,
    private val inventory: ShipmentInventoryPort,
    private val tracking: ShipmentTrackingPort,
) : ShipmentUseCase {
    override fun registerTracking(orderId: UUID, carrierCode: String, trackingNumber: String, operatorId: UUID): ShipmentChange {
        val carrier = carrierCode.trim()
        val tracking = trackingNumber.trim()
        require(carrier.isNotEmpty() && carrier.length <= MAX_CARRIER_LENGTH) { "택배사 코드는 1~80자여야 합니다." }
        require(tracking.isNotEmpty() && tracking.length <= MAX_TRACKING_LENGTH) { "송장번호는 1~100자여야 합니다." }
        val current = shipments.lock(orderId) ?: throw ItemNotFoundException("주문 배송 정보를 찾을 수 없습니다.")
        if (current.status == IN_TRANSIT && current.carrierCode == carrier && current.trackingNumber == tracking) {
            return current.toChange(false)
        }
        require(current.status == PREPARING || current.status == IN_TRANSIT) { "발송 처리 중인 주문만 송장 정보를 등록할 수 있습니다." }
        val changed = shipments.update(current, IN_TRANSIT, carrier, tracking, operatorId)
        return changed.toChange(true)
    }

    override fun markDelivered(orderId: UUID, operatorId: UUID): ShipmentChange {
        val current = shipments.lock(orderId) ?: throw ItemNotFoundException("주문 배송 정보를 찾을 수 없습니다.")
        if (current.status == DELIVERED) return current.toChange(false)
        require(current.status == IN_TRANSIT) { "배송 중인 주문만 배송 완료 처리할 수 있습니다." }
        return shipments.update(current, DELIVERED, current.carrierCode, current.trackingNumber, operatorId).toChange(true)
    }

    override fun refreshForCustomer(customerId: UUID): Int = shipments.trackingCandidatesForCustomer(customerId).count { candidate ->
        synchronize(candidate).changed
    }

    private fun synchronize(candidate: ShipmentTrackingCandidate): ShipmentChange {
        if (candidate.status != IN_TRANSIT) return candidate.toChange(false)
        val lookup = tracking.lookup(candidate.carrierCode, candidate.trackingNumber)
        if (lookup == CarrierTrackingStatus.DELIVERED && shipments.markDeliveredIfCurrent(candidate)) {
            return candidate.toChange(true, DELIVERED)
        }
        return candidate.toChange(false)
    }

    override fun prepareReadyOrders(): Int {
        var prepared = 0
        shipments.readyOrderIds().forEach { orderId ->
            if (prepareOrder(orderId).changed) prepared++
        }
        return prepared
    }

    override fun prepareOrder(orderId: UUID): ShipmentChange {
        val current = shipments.lock(orderId) ?: throw ItemNotFoundException("주문 배송 정보를 찾을 수 없습니다.")
        if (current.status == PREPARING) return current.toChange(false)
        require(current.status == READY_TO_SHIP) { "배송 준비 가능한 주문 상태가 아닙니다." }
        current.reservationKeys.forEach(inventory::confirm)
        return shipments.update(current, PREPARING, null, null, null).toChange(true)
    }

    private fun com.buyeong.umji.api.shipment.application.model.ShipmentRecord.toChange(changed: Boolean) =
        ShipmentChange(orderId, status, carrierCode, trackingNumber, changed)

    private fun ShipmentTrackingCandidate.toChange(
        changed: Boolean,
        status: String = this.status,
    ) = ShipmentChange(orderId, status, carrierCode, trackingNumber, changed)

    private companion object {
        const val READY_TO_SHIP = "READY_TO_SHIP"
        const val PREPARING = "PREPARING"
        const val IN_TRANSIT = "IN_TRANSIT"
        const val DELIVERED = "DELIVERED"
        const val MAX_CARRIER_LENGTH = 80
        const val MAX_TRACKING_LENGTH = 100
    }
}