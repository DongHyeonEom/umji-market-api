package com.buyeong.umji.api.payment.adapter.out.persistence

import com.buyeong.umji.api.payment.application.model.PaymentQueueItem
import com.buyeong.umji.api.payment.application.model.PaymentQueuePage
import com.buyeong.umji.api.payment.application.model.PaymentRecord
import com.buyeong.umji.api.payment.application.model.PaymentStatusChange
import com.buyeong.umji.api.payment.application.port.out.PaymentStorePort
import com.buyeong.umji.api.persistence.jpa.order.OrderPaymentEntity
import com.buyeong.umji.api.persistence.jpa.order.OrderPaymentJpaEntityService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class JpaPaymentAdapter(private val payments: OrderPaymentJpaEntityService) : PaymentStorePort {
    override fun queue(status: String?, page: Int, size: Int): PaymentQueuePage {
        val statuses = status?.let(::setOf) ?: setOf(WAITING_FOR_DEPOSIT, PARTIAL_PAYMENT_REVIEW_REQUIRED, PAYMENT_ISSUE_REVIEW_REQUIRED, REFUND_PENDING)
        val result = payments.findAllForOperation(statuses, PageRequest.of(page, size, Sort.by("updatedAt").ascending()))
        return PaymentQueuePage(
            result.content.map { it.toQueueItem() },
            result.number,
            result.size,
            result.totalElements,
            result.totalPages,
        )
    }

    override fun lock(orderId: UUID): PaymentRecord? = payments.findForUpdate(orderId)?.let { payment ->
        PaymentRecord(
            orderId = requireNotNull(payment.order.publicId),
            orderStatus = payment.order.status,
            paymentStatus = payment.status,
        )
    }

    override fun updateStatus(record: PaymentRecord, status: String, operatorId: UUID): PaymentStatusChange {
        val payment = requireNotNull(payments.findForUpdate(record.orderId)) { "결제를 찾을 수 없습니다." }
        val changed = payments.saveChange(payment, status, operatorId)
        return PaymentStatusChange(
            orderId = record.orderId,
            orderStatus = changed.order.status,
            paymentStatus = changed.status,
            changed = true,
        )
    }

    private fun OrderPaymentEntity.toQueueItem() = PaymentQueueItem(
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
        const val WAITING_FOR_DEPOSIT = "WAITING_FOR_DEPOSIT"
        const val PARTIAL_PAYMENT_REVIEW_REQUIRED = "PARTIAL_PAYMENT_REVIEW_REQUIRED"
        const val PAYMENT_ISSUE_REVIEW_REQUIRED = "PAYMENT_ISSUE_REVIEW_REQUIRED"
        const val REFUND_PENDING = "REFUND_PENDING"
        const val PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED"
    }
}