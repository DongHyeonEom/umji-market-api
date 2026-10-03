package com.buyeong.umji.api.payment.application

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.notification.application.model.NotificationEventType
import com.buyeong.umji.api.notification.application.port.`in`.NoOpNotificationEventUseCase
import com.buyeong.umji.api.notification.application.port.`in`.NotificationEventUseCase
import com.buyeong.umji.api.payment.application.model.PaymentQueuePage
import com.buyeong.umji.api.payment.application.model.PaymentStatusChange
import com.buyeong.umji.api.payment.application.port.`in`.PaymentUseCase
import com.buyeong.umji.api.payment.application.port.out.PaymentStorePort
import java.util.UUID

class PaymentService(
    private val payments: PaymentStorePort,
    private val notifications: NotificationEventUseCase = NoOpNotificationEventUseCase,
) : PaymentUseCase {
    override fun queue(status: String?, page: Int, size: Int): PaymentQueuePage {
        require(status == null || status in PAYMENT_STATUSES) { "유효하지 않은 결제 상태입니다." }
        require(page >= 0) { "페이지 번호는 0 이상이어야 합니다." }
        require(size in 1..100) { "페이지 크기는 1~100이어야 합니다." }
        return payments.queue(status, page, size)
    }

    override fun updateStatus(orderId: UUID, status: String, operatorId: UUID): PaymentStatusChange {
        require(status in PAYMENT_STATUSES) { "유효하지 않은 결제 상태입니다." }
        val current = payments.lock(orderId) ?: throw ItemNotFoundException("결제를 찾을 수 없습니다.")
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
            return PaymentStatusChange(current.orderId, current.orderStatus, current.paymentStatus, false)
        }

        val updated = payments.updateStatus(current, status, operatorId)
        if (updated.changed) notifications.record(NotificationEventType.PAYMENT_STATUS_CHANGED, updated.orderId, updated.paymentStatus)
        return updated
    }

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
    }
}