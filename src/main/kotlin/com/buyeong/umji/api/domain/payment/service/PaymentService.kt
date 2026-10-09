package com.buyeong.umji.api.domain.payment.service

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.domain.notification.dto.NotificationEventType
import com.buyeong.umji.api.domain.notification.service.NotificationEventService
import com.buyeong.umji.api.domain.payment.dto.PaymentQueueItemDto
import com.buyeong.umji.api.domain.payment.dto.PaymentQueuePageDto
import com.buyeong.umji.api.domain.payment.dto.PaymentRecordDto
import com.buyeong.umji.api.domain.payment.dto.PaymentStatusChangeDto
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderPaymentEntity
import com.buyeong.umji.api.persistence.jpa.order.service.OrderPaymentJpaEntityService
import com.buyeong.umji.api.domain.sales.service.SalesCommissionService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PaymentService(
    private val payments: OrderPaymentJpaEntityService,
    private val notifications: NotificationEventService,
    private val salesCommissions: SalesCommissionService,
) {
    fun queue(status: String?, page: Int, size: Int): PaymentQueuePageDto {
        require(status == null || status in PAYMENT_STATUSES) { "유효하지 않은 결제 상태입니다." }
        require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
        require(size in 1..100) { "페이지 크기는 1~100이어야 합니다." }
        val statuses = status?.let(::setOf) ?: PAYMENT_QUEUE_STATUSES
        val result = payments.findAllForOperation(statuses, PageRequest.of(page, size, Sort.by("updatedAt").ascending()))
        return PaymentQueuePageDto(
            result.content.map { it.toQueueItem() },
            result.number,
            result.size,
            result.totalElements,
            result.totalPages,
        )
    }

    @Transactional
    fun updateStatus(orderId: UUID, status: String, operatorId: UUID): PaymentStatusChangeDto {
        require(status in PAYMENT_STATUSES) { "유효하지 않은 결제 상태입니다." }
        val payment = payments.findForUpdate(orderId) ?: throw ItemNotFoundException("결제를 찾을 수 없습니다.")
        val current = PaymentRecordDto(requireNotNull(payment.order.publicId), payment.order.status, payment.status)
        require(status !in setOf(REFUND_PENDING, REFUNDED) || current.orderStatus == ORDER_CANCELLED) {
            "취소된 주문만 환불 상태로 변경할 수 있습니다."
        }
        require(
            when (current.paymentStatus) {
                PAYMENT_CONFIRMED -> status in setOf(PAYMENT_CONFIRMED, REFUND_PENDING)
                REFUND_PENDING -> status in setOf(REFUND_PENDING, REFUNDED)
                REFUNDED -> status == REFUNDED
                else -> status !in setOf(REFUND_PENDING, REFUNDED)
            },
        ) { "허용되지 않는 결제·환불 상태 변경입니다." }
        if (current.paymentStatus == status) {
            return PaymentStatusChangeDto(current.orderId, current.orderStatus, current.paymentStatus, false)
        }

        val updatedPayment = payments.saveChange(payment, status, operatorId)
        if (status == REFUNDED) {
            salesCommissions.reverseOrder(orderId, "ORDER_REFUND")
        }
        val updated = PaymentStatusChangeDto(requireNotNull(updatedPayment.order.publicId), updatedPayment.order.status, updatedPayment.status, true)
        if (updated.changed) notifications.record(NotificationEventType.PAYMENT_STATUS_CHANGED, updated.orderId, updated.paymentStatus)
        return updated
    }

    private fun OrderPaymentEntity.toQueueItem() = PaymentQueueItemDto(
        orderId = requireNotNull(order.publicId),
        orderNumber = order.orderNumber,
        customerName = order.account.name,
        customerPhone = order.account.phone,
        orderAmount = order.totalAmount,
        paymentMethod = paymentMethod,
        paymentStatus = status,
        updatedAt = updatedAt,
    )

    private companion object {
        const val PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED"
        const val REFUND_PENDING = "REFUND_PENDING"
        const val REFUNDED = "REFUNDED"
        const val ORDER_CANCELLED = "CANCELLED"
        val PAYMENT_STATUSES = setOf(
            "WAITING_FOR_DEPOSIT",
            "PARTIAL_PAYMENT_REVIEW_REQUIRED",
            "PAYMENT_ISSUE_REVIEW_REQUIRED",
            PAYMENT_CONFIRMED,
            REFUND_PENDING,
            REFUNDED,
        )
        val PAYMENT_QUEUE_STATUSES = setOf("WAITING_FOR_DEPOSIT", "PARTIAL_PAYMENT_REVIEW_REQUIRED", "PAYMENT_ISSUE_REVIEW_REQUIRED", REFUND_PENDING)
    }
}