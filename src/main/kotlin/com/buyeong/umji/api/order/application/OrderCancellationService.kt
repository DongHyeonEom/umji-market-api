package com.buyeong.umji.api.order.application

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.inventory.application.port.`in`.InventoryUseCase
import com.buyeong.umji.api.order.application.model.CancellationChange
import com.buyeong.umji.api.order.application.model.CancellationQueuePage
import com.buyeong.umji.api.order.application.port.`in`.OrderCancellationUseCase
import com.buyeong.umji.api.order.application.port.out.OrderCancellationPort
import java.util.UUID

class OrderCancellationService(
    private val cancellations: OrderCancellationPort,
    private val inventory: InventoryUseCase,
) : OrderCancellationUseCase {
    override fun request(accountId: UUID, orderId: UUID): CancellationChange {
        val order = cancellations.lock(orderId) ?: throw ItemNotFoundException("주문을 찾을 수 없습니다.")
        require(order.accountId == accountId) { "본인 주문만 취소할 수 있습니다." }
        require(order.orderStatus != CANCELLED) { "이미 취소된 주문입니다." }
        when (order.shippingStatus) {
            READY_TO_SHIP -> {
                order.reservationKeys.forEach(inventory::release)
                cancellations.cancel(order, null)
                cancellations.recordRequest(orderId, accountId, CANCELLED)
                return CancellationChange(orderId, CANCELLED, CANCELLED)
            }
            PREPARING -> {
                require(!cancellations.hasPendingRequest(orderId)) { "이미 취소 요청이 접수되었습니다." }
                cancellations.recordRequest(orderId, accountId, PENDING)
                return CancellationChange(orderId, order.orderStatus, PENDING)
            }
            IN_TRANSIT -> throw IllegalArgumentException("배송 중인 주문은 취소할 수 없습니다.")
            else -> throw IllegalArgumentException("취소할 수 없는 배송 상태입니다.")
        }
    }

    override fun queue(page: Int, size: Int): CancellationQueuePage {
        require(page >= 0 && size in 1..100) { "페이지 값이 올바르지 않습니다." }
        return cancellations.queue(page, size)
    }

    override fun resolve(orderId: UUID, approved: Boolean, operatorId: UUID): CancellationChange {
        val order = cancellations.lock(orderId) ?: throw ItemNotFoundException("주문을 찾을 수 없습니다.")
        require(order.shippingStatus == PREPARING) { "배송 준비 상태의 주문만 취소 요청을 처리할 수 있습니다." }
        require(cancellations.hasPendingRequest(orderId)) { "대기 중인 취소 요청이 없습니다." }
        val status = if (approved) APPROVED else REJECTED
        if (approved) {
            order.reservationKeys.forEach(inventory::restoreConfirmed)
            cancellations.cancel(order, operatorId)
        }
        cancellations.resolveRequest(orderId, status, operatorId)
        return CancellationChange(orderId, if (approved) CANCELLED else order.orderStatus, status)
    }

    private companion object {
        const val READY_TO_SHIP = "READY_TO_SHIP"
        const val PREPARING = "PREPARING"
        const val IN_TRANSIT = "IN_TRANSIT"
        const val CANCELLED = "CANCELLED"
        const val PENDING = "PENDING"
        const val APPROVED = "APPROVED"
        const val REJECTED = "REJECTED"
    }
}