package com.buyeong.umji.api.order.service

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.inventory.service.InventoryService
import com.buyeong.umji.api.notification.dto.NotificationEventType
import com.buyeong.umji.api.notification.service.NotificationEventService
import com.buyeong.umji.api.order.dto.CancellationChangeDto
import com.buyeong.umji.api.order.dto.CancellationQueuePageDto
import com.buyeong.umji.api.persistence.jpa.order.service.OrderCancellationJpaEntityService
import com.buyeong.umji.api.sales.service.SalesCommissionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class OrderCancellationService(
    private val cancellations: OrderCancellationJpaEntityService,
    private val inventory: InventoryService,
    private val notifications: NotificationEventService,
    private val salesCommissions: SalesCommissionService,
) {
    fun request(accountId: UUID, orderId: UUID): CancellationChangeDto {
        val order = cancellations.lock(orderId) ?: throw ItemNotFoundException("주문을 찾을 수 없습니다.")
        require(order.accountId == accountId) { "본인 주문만 취소할 수 있습니다." }
        require(order.orderStatus != CANCELLED) { "이미 취소된 주문입니다." }
        when (order.shippingStatus) {
            READY_TO_SHIP -> {
                order.reservationKeys.forEach(inventory::release)
                cancellations.cancel(order, null)
                salesCommissions.reverseOrder(orderId, "ORDER_CANCELLED")
                cancellations.recordRequest(orderId, accountId, CANCELLED)
                notifications.record(NotificationEventType.ORDER_CANCELLED, orderId)
                return CancellationChangeDto(orderId, CANCELLED, CANCELLED)
            }
            PREPARING -> {
                require(!cancellations.hasPendingRequest(orderId)) { "이미 취소 요청이 접수되었습니다." }
                cancellations.recordRequest(orderId, accountId, PENDING)
                return CancellationChangeDto(orderId, order.orderStatus, PENDING)
            }
            IN_TRANSIT -> throw IllegalArgumentException("배송 중인 주문은 취소할 수 없습니다.")
            else -> throw IllegalArgumentException("취소할 수 없는 배송 상태입니다.")
        }
    }

    @Transactional(readOnly = true)
    fun queue(page: Int, size: Int): CancellationQueuePageDto {
        require(page >= 0 && size in 1..100) { "페이지 값이 올바르지 않습니다." }
        return cancellations.queue(page, size)
    }

    fun resolve(orderId: UUID, approved: Boolean, operatorId: UUID): CancellationChangeDto {
        val order = cancellations.lock(orderId) ?: throw ItemNotFoundException("주문을 찾을 수 없습니다.")
        require(order.shippingStatus == PREPARING) { "배송 준비 상태의 주문만 취소 요청을 처리할 수 있습니다." }
        require(cancellations.hasPendingRequest(orderId)) { "대기 중인 취소 요청이 없습니다." }
        val status = if (approved) APPROVED else REJECTED
        if (approved) {
            order.reservationKeys.forEach(inventory::restoreConfirmed)
            cancellations.cancel(order, operatorId)
            salesCommissions.reverseOrder(orderId, "ORDER_CANCELLED")
            notifications.record(NotificationEventType.ORDER_CANCELLED, orderId)
        }
        cancellations.resolveRequest(orderId, status, operatorId)
        return CancellationChangeDto(orderId, if (approved) CANCELLED else order.orderStatus, status)
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