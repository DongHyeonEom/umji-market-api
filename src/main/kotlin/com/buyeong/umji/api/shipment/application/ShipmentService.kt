package com.buyeong.umji.api.shipment.application

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.shipment.application.model.ShipmentChange
import com.buyeong.umji.api.shipment.application.port.`in`.ShipmentUseCase
import com.buyeong.umji.api.shipment.application.port.out.ShipmentInventoryPort
import com.buyeong.umji.api.shipment.application.port.out.ShipmentStorePort
import java.util.UUID

class ShipmentService(
    private val shipments: ShipmentStorePort,
    private val inventory: ShipmentInventoryPort,
) : ShipmentUseCase {
    override fun beginDispatch(orderId: UUID, operatorId: UUID): ShipmentChange {
        val current = shipments.lock(orderId) ?: throw ItemNotFoundException("주문 배송 정보를 찾을 수 없습니다.")
        if (current.status == PREPARING) return current.toChange(false)
        require(current.status == READY_TO_SHIP) { "발송 준비 상태의 주문만 발송 처리를 시작할 수 있습니다." }
        return shipments.update(current, PREPARING, null, null, operatorId).toChange(true)
    }

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
        if (current.status != IN_TRANSIT) current.reservationKeys.forEach(inventory::confirm)
        return changed.toChange(true)
    }

    private fun com.buyeong.umji.api.shipment.application.model.ShipmentRecord.toChange(changed: Boolean) =
        ShipmentChange(orderId, status, carrierCode, trackingNumber, changed)

    private companion object {
        const val READY_TO_SHIP = "READY_TO_SHIP"
        const val PREPARING = "PREPARING"
        const val IN_TRANSIT = "IN_TRANSIT"
        const val MAX_CARRIER_LENGTH = 80
        const val MAX_TRACKING_LENGTH = 100
    }
}