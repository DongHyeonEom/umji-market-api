package com.buyeong.umji.api.payment.service

import com.buyeong.umji.api.notification.model.NotificationEventType
import com.buyeong.umji.api.notification.service.NotificationEventService
import com.buyeong.umji.api.persistence.jpa.order.entity.OrderPaymentEntity
import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderEntity
import com.buyeong.umji.api.persistence.jpa.order.service.OrderPaymentJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class PaymentServiceTest : DescribeSpec({
    val payments = mockk<OrderPaymentJpaEntityService>()
    val notifications = mockk<NotificationEventService>(relaxed = true)
    val service = PaymentService(payments, notifications)
    val orderId = UUID.randomUUID()
    val operatorId = UUID.randomUUID()

    describe("운영자 입금 상태 변경") {
        it("부분 입금 확인 필요 상태를 결제 상태로만 저장한다") {
            clearMocks(payments)
            clearMocks(notifications)
            val current = payment(orderId, "PENDING_PAYMENT", "WAITING_FOR_DEPOSIT")
            every { payments.findForUpdate(orderId) } returns current
            every { payments.saveChange(current, "PARTIAL_PAYMENT_REVIEW_REQUIRED", operatorId) } answers {
                payment(orderId, "PENDING_PAYMENT", "PARTIAL_PAYMENT_REVIEW_REQUIRED")
            }

            service.updateStatus(orderId, "PARTIAL_PAYMENT_REVIEW_REQUIRED", operatorId).paymentStatus shouldBe "PARTIAL_PAYMENT_REVIEW_REQUIRED"
            verify(exactly = 1) { notifications.record(NotificationEventType.PAYMENT_STATUS_CHANGED, orderId, "PARTIAL_PAYMENT_REVIEW_REQUIRED") }
        }

        it("전액 입금 확인 상태를 배송과 별도로 저장한다") {
            clearMocks(payments)
            clearMocks(notifications)
            val current = payment(orderId, "PENDING_PAYMENT", "PARTIAL_PAYMENT_REVIEW_REQUIRED")
            every { payments.findForUpdate(orderId) } returns current
            every { payments.saveChange(current, "PAYMENT_CONFIRMED", operatorId) } answers {
                payment(orderId, "PAID", "PAYMENT_CONFIRMED")
            }

            service.updateStatus(orderId, "PAYMENT_CONFIRMED", operatorId).orderStatus shouldBe "PAID"
            verify(exactly = 1) { notifications.record(NotificationEventType.PAYMENT_STATUS_CHANGED, orderId, "PAYMENT_CONFIRMED") }
        }

        it("같은 입금 상태 요청은 중복 변경하지 않는다") {
            clearMocks(payments)
            clearMocks(notifications)
            every { payments.findForUpdate(orderId) } returns payment(orderId, "PAID", "PAYMENT_CONFIRMED")

            val result = service.updateStatus(orderId, "PAYMENT_CONFIRMED", operatorId)

            result.changed shouldBe false
            io.mockk.verify(exactly = 0) { payments.saveChange(any(), any(), any()) }
            verify(exactly = 0) { notifications.record(any(), any(), any()) }
        }

        it("지원하지 않는 결제 상태를 거절한다") {
            clearMocks(payments)
            shouldThrow<IllegalArgumentException> {
                service.updateStatus(orderId, "NOT_A_STATUS", operatorId)
            }
        }

        it("취소되지 않은 주문은 환불 상태로 변경할 수 없다") {
            clearMocks(payments)
            every { payments.findForUpdate(orderId) } returns payment(orderId, "PAID", "REFUND_PENDING")
            shouldThrow<IllegalArgumentException> { service.updateStatus(orderId, "REFUNDED", operatorId) }
        }
    }

})

private fun payment(orderId: UUID, orderStatus: String, paymentStatus: String) = OrderPaymentEntity().apply {
    order = mockk<PurchaseOrderEntity> {
        every { publicId } returns orderId
        every { status } returns orderStatus
    }
    status = paymentStatus
}
