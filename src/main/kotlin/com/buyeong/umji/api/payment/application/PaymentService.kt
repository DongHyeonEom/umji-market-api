package com.buyeong.umji.api.payment.application

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.payment.application.model.PaymentQueuePage
import com.buyeong.umji.api.payment.application.model.PaymentStatusChange
import com.buyeong.umji.api.payment.application.port.`in`.PaymentUseCase
import com.buyeong.umji.api.payment.application.port.out.PaymentInventoryPort
import com.buyeong.umji.api.payment.application.port.out.PaymentStorePort
import java.util.UUID

class PaymentService(
    private val payments: PaymentStorePort,
    private val inventory: PaymentInventoryPort,
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
        require(current.orderStatus == ORDER_PENDING_PAYMENT || current.paymentStatus == PAYMENT_CONFIRMED) {
            "결제 대기 중인 주문만 입금 상태를 변경할 수 있습니다."
        }
        require(current.paymentStatus != PAYMENT_CONFIRMED || status == PAYMENT_CONFIRMED) {
            "입금 확인 완료 상태는 되돌릴 수 없습니다."
        }
        if (current.paymentStatus == status) {
            return PaymentStatusChange(current.orderId, current.orderStatus, current.paymentStatus, false, emptyList())
        }

        val changed = payments.updateStatus(current, status, operatorId)
        if (changed.changed && changed.paymentStatus == PAYMENT_CONFIRMED) {
            changed.reservationKeys.forEach(inventory::confirm)
        }
        return changed
    }

    private companion object {
        const val ORDER_PENDING_PAYMENT = "PENDING_PAYMENT"
        const val PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED"
        val PAYMENT_STATUSES = setOf("WAITING_FOR_DEPOSIT", "PARTIAL_PAYMENT_REVIEW_REQUIRED", PAYMENT_CONFIRMED)
    }
}
