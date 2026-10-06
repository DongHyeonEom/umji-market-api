package com.buyeong.umji.api.persistence.jpa.order.service

import com.buyeong.umji.api.order.model.CancellationOrder
import com.buyeong.umji.api.order.model.CancellationQueueItem
import com.buyeong.umji.api.order.model.CancellationQueuePage
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderCancellationHistoryEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderStatusHistoryEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderEntity
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderCancellationHistoryRepository
import com.buyeong.umji.api.persistence.jpa.order.repository.OrderStatusHistoryRepository
import com.buyeong.umji.api.persistence.jpa.order.repository.PurchaseOrderRepository
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrderCancellationJpaEntityService(
    private val orders: PurchaseOrderRepository,
    private val histories: OrderStatusHistoryRepository,
    private val cancellations: OrderCancellationHistoryRepository,
    private val payments: OrderPaymentJpaEntityService,
    private val accounts: AccountJpaEntityService,
) {
    @Transactional
    fun lock(orderId: UUID): CancellationOrder? = orders.findForCancellation(orderId)?.toCancellationOrder()

    fun hasPendingRequest(orderId: UUID): Boolean = cancellations.existsByOrderPublicIdAndStatus(orderId, PENDING)

    @Transactional
    fun recordRequest(orderId: UUID, requesterId: UUID, status: String, processorId: UUID? = null) {
        val order = requireNotNull(orders.findForCancellation(orderId))
        val requester = requireNotNull(accounts.findByPublicId(requesterId))
        cancellations.saveAndFlush(
            OrderCancellationHistoryEntity().apply {
                this.order = order
                this.requester = requester
                this.status = status
                this.processor = processorId?.let(accounts::findByPublicId)
                requestedAt = Instant.now()
                processedAt = if (status == CANCELLED) requestedAt else null
            },
        )
    }

    @Transactional
    fun cancel(order: CancellationOrder, processorId: UUID?) {
        val entity = requireNotNull(orders.findForCancellation(order.id))
        check(entity.status != CANCELLED) { "이미 취소된 주문입니다." }
        val now = Instant.now()
        val previous = entity.status
        entity.status = CANCELLED
        orders.saveAndFlush(entity)
        histories.save(
            OrderStatusHistoryEntity().apply {
                this.order = entity
                fromStatus = previous
                toStatus = CANCELLED
                reasonCode = ORDER_CANCELLED
                changedAt = now
            },
        )
        val payment = payments.findForUpdate(order.id) ?: error("주문 결제 정보를 찾을 수 없습니다.")
        if (payment.status != WAITING_FOR_DEPOSIT && payment.status != REFUNDED && payment.status != REFUND_PENDING) {
            payments.saveChange(payment, REFUND_PENDING, processorId ?: order.accountId)
        }
    }

    @Transactional
    fun resolveRequest(orderId: UUID, status: String, processorId: UUID) {
        val history = cancellations.findPending(orderId).firstOrNull() ?: error("대기 중인 취소 요청이 없습니다.")
        history.status = status
        history.processor = accounts.findByPublicId(processorId) ?: error("운영자 계정을 찾을 수 없습니다.")
        history.processedAt = Instant.now()
        cancellations.saveAndFlush(history)
    }

    fun queue(page: Int, size: Int): CancellationQueuePage {
        val result = cancellations.findQueue(PENDING, PageRequest.of(page, size, Sort.by("requestedAt").ascending()))
        return CancellationQueuePage(
            result.content.map {
                CancellationQueueItem(requireNotNull(it.order.publicId), it.order.orderNumber, requireNotNull(it.requester.publicId), it.requestedAt)
            },
            result.number,
            result.size,
            result.totalElements,
            result.totalPages,
        )
    }

    private fun PurchaseOrderEntity.toCancellationOrder() = CancellationOrder(
        id = requireNotNull(publicId),
        accountId = requireNotNull(account.publicId),
        orderNumber = orderNumber,
        orderStatus = status,
        paymentStatus = payment.status,
        shippingStatus = shipment.status,
        reservationKeys = items.map { it.reservationKey },
    )

    private companion object {
        const val PENDING = "PENDING"
        const val CANCELLED = "CANCELLED"
        const val ORDER_CANCELLED = "ORDER_CANCELLED"
        const val WAITING_FOR_DEPOSIT = "WAITING_FOR_DEPOSIT"
        const val REFUNDED = "REFUNDED"
        const val REFUND_PENDING = "REFUND_PENDING"
    }
}
