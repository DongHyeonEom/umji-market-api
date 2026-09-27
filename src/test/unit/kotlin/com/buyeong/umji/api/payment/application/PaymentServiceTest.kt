package com.buyeong.umji.api.payment.application

import com.buyeong.umji.api.payment.application.model.PaymentRecord
import com.buyeong.umji.api.payment.application.model.PaymentStatusChange
import com.buyeong.umji.api.payment.application.port.out.PaymentInventoryPort
import com.buyeong.umji.api.payment.application.port.out.PaymentStorePort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class PaymentServiceTest : DescribeSpec({
    val payments = mockk<PaymentStorePort>()
    val inventory = mockk<PaymentInventoryPort>(relaxed = true)
    val service = PaymentService(payments, inventory)
    val orderId = UUID.randomUUID()
    val operatorId = UUID.randomUUID()
    val reservationKey = UUID.randomUUID()

    describe("운영자 입금 상태 변경") {
        it("부분 입금 확인 필요 상태에서는 주문과 재고를 확정하지 않는다") {
            clearMocks(payments, inventory)
            val current = PaymentRecord(orderId, "PENDING_PAYMENT", "WAITING_FOR_DEPOSIT", listOf(reservationKey))
            val result = PaymentStatusChange(orderId, "PENDING_PAYMENT", "PARTIAL_PAYMENT_REVIEW_REQUIRED", true, emptyList())
            every { payments.lock(orderId) } returns current
            every { payments.updateStatus(current, "PARTIAL_PAYMENT_REVIEW_REQUIRED", operatorId) } returns result

            service.updateStatus(orderId, "PARTIAL_PAYMENT_REVIEW_REQUIRED", operatorId) shouldBe result

            verify(exactly = 0) { inventory.confirm(any()) }
        }

        it("전액 입금 확인 시 모든 재고 예약을 확정한다") {
            clearMocks(payments, inventory)
            val current = PaymentRecord(orderId, "PENDING_PAYMENT", "PARTIAL_PAYMENT_REVIEW_REQUIRED", listOf(reservationKey))
            val result = PaymentStatusChange(orderId, "PAID", "PAYMENT_CONFIRMED", true, listOf(reservationKey))
            every { payments.lock(orderId) } returns current
            every { payments.updateStatus(current, "PAYMENT_CONFIRMED", operatorId) } returns result

            service.updateStatus(orderId, "PAYMENT_CONFIRMED", operatorId) shouldBe result

            verify(exactly = 1) { inventory.confirm(reservationKey) }
        }

        it("같은 입금 상태 요청은 중복 재고 확정을 하지 않는다") {
            clearMocks(payments, inventory)
            val current = PaymentRecord(orderId, "PAID", "PAYMENT_CONFIRMED", listOf(reservationKey))
            every { payments.lock(orderId) } returns current

            val result = service.updateStatus(orderId, "PAYMENT_CONFIRMED", operatorId)

            result.changed shouldBe false
            verify(exactly = 0) { payments.updateStatus(any(), any(), any()) }
            verify(exactly = 0) { inventory.confirm(any()) }
        }

        it("지원하지 않는 결제 상태를 거절한다") {
            clearMocks(payments, inventory)
            shouldThrow<IllegalArgumentException> {
                service.updateStatus(orderId, "REFUNDED", operatorId)
            }
        }
    }
})
