package com.buyeong.umji.api.payment.application

import com.buyeong.umji.api.payment.application.model.PaymentRecord
import com.buyeong.umji.api.payment.application.model.PaymentStatusChange
import com.buyeong.umji.api.payment.application.port.out.PaymentStorePort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.util.UUID

class PaymentServiceTest : DescribeSpec({
    val payments = mockk<PaymentStorePort>()
    val service = PaymentService(payments)
    val orderId = UUID.randomUUID()
    val operatorId = UUID.randomUUID()

    describe("운영자 입금 상태 변경") {
        it("부분 입금 확인 필요 상태를 결제 상태로만 저장한다") {
            clearMocks(payments)
            val current = PaymentRecord(orderId, "PENDING_PAYMENT", "WAITING_FOR_DEPOSIT")
            val result = PaymentStatusChange(orderId, "PENDING_PAYMENT", "PARTIAL_PAYMENT_REVIEW_REQUIRED", true)
            every { payments.lock(orderId) } returns current
            every { payments.updateStatus(current, "PARTIAL_PAYMENT_REVIEW_REQUIRED", operatorId) } returns result

            service.updateStatus(orderId, "PARTIAL_PAYMENT_REVIEW_REQUIRED", operatorId) shouldBe result
        }

        it("전액 입금 확인 상태를 배송과 별도로 저장한다") {
            clearMocks(payments)
            val current = PaymentRecord(orderId, "PENDING_PAYMENT", "PARTIAL_PAYMENT_REVIEW_REQUIRED")
            val result = PaymentStatusChange(orderId, "PAID", "PAYMENT_CONFIRMED", true)
            every { payments.lock(orderId) } returns current
            every { payments.updateStatus(current, "PAYMENT_CONFIRMED", operatorId) } returns result

            service.updateStatus(orderId, "PAYMENT_CONFIRMED", operatorId) shouldBe result
        }

        it("같은 입금 상태 요청은 중복 변경하지 않는다") {
            clearMocks(payments)
            val current = PaymentRecord(orderId, "PAID", "PAYMENT_CONFIRMED")
            every { payments.lock(orderId) } returns current

            val result = service.updateStatus(orderId, "PAYMENT_CONFIRMED", operatorId)

            result.changed shouldBe false
            io.mockk.verify(exactly = 0) { payments.updateStatus(any(), any(), any()) }
        }

        it("지원하지 않는 결제 상태를 거절한다") {
            clearMocks(payments)
            shouldThrow<IllegalArgumentException> {
                service.updateStatus(orderId, "NOT_A_STATUS", operatorId)
            }
        }

        it("취소되지 않은 주문은 환불 상태로 변경할 수 없다") {
            clearMocks(payments)
            every { payments.lock(orderId) } returns PaymentRecord(orderId, "PAID", "REFUND_PENDING")
            shouldThrow<IllegalArgumentException> { service.updateStatus(orderId, "REFUNDED", operatorId) }
        }
    }
})