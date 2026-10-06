package com.buyeong.umji.api.order.service

import com.buyeong.umji.api.inventory.service.InventoryService
import com.buyeong.umji.api.notification.model.NotificationEventType
import com.buyeong.umji.api.notification.service.NotificationEventService
import com.buyeong.umji.api.order.model.CancellationOrder
import com.buyeong.umji.api.persistence.jpa.order.service.OrderCancellationJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class OrderCancellationServiceTest : DescribeSpec({
    val port = mockk<OrderCancellationJpaEntityService>(relaxed = true)
    val inventory = mockk<InventoryService>(relaxed = true)
    val notifications = mockk<NotificationEventService>(relaxed = true)
    val service = OrderCancellationService(port, inventory, notifications)
    val customer = UUID.randomUUID()
    val orderId = UUID.randomUUID()
    val reservation = UUID.randomUUID()
    fun order(status: String) = CancellationOrder(orderId, customer, "UMJ-20260930-000001", "PENDING_PAYMENT", "WAITING_FOR_DEPOSIT", status, listOf(reservation))

    beforeTest { io.mockk.clearMocks(port, inventory, notifications, answers = false, recordedCalls = true, childMocks = true, verificationMarks = true, exclusionRules = true) }

    it("배송 준비 전 전체 취소는 예약 재고를 풀고 주문 취소 이력을 남긴다") {
        every { port.lock(orderId) } returns order("READY_TO_SHIP")
        service.request(customer, orderId).requestStatus shouldBe "CANCELLED"
        verify { inventory.release(reservation) }
        verify { port.cancel(any(), null) }
        verify { port.recordRequest(orderId, customer, "CANCELLED", null) }
        verify(exactly = 1) { notifications.record(NotificationEventType.ORDER_CANCELLED, orderId, null) }
    }

    it("배송 준비 후 송장 등록 전 취소는 운영자 확인 요청으로 접수한다") {
        every { port.lock(orderId) } returns order("PREPARING")
        every { port.hasPendingRequest(orderId) } returns false
        service.request(customer, orderId).requestStatus shouldBe "PENDING"
        verify { port.recordRequest(orderId, customer, "PENDING", null) }
        verify(exactly = 0) { notifications.record(any(), any(), any()) }
        verify(exactly = 0) { inventory.restoreConfirmed(any()) }
    }

    it("운영자 승인 시 확정 재고를 복구하고 취소 상태를 기록한다") {
        every { port.lock(orderId) } returns order("PREPARING")
        every { port.hasPendingRequest(orderId) } returns true
        service.resolve(orderId, true, UUID.randomUUID()).requestStatus shouldBe "APPROVED"
        verify { inventory.restoreConfirmed(reservation) }
        verify { port.cancel(any(), any()) }
        verify { port.resolveRequest(orderId, "APPROVED", any()) }
        verify(exactly = 1) { notifications.record(NotificationEventType.ORDER_CANCELLED, orderId, null) }
    }

    it("운영자 거절 시 취소 상태와 재고를 유지한다") {
        every { port.lock(orderId) } returns order("PREPARING")
        every { port.hasPendingRequest(orderId) } returns true
        service.resolve(orderId, false, UUID.randomUUID()).requestStatus shouldBe "REJECTED"
        verify(exactly = 0) { inventory.restoreConfirmed(any()) }
        verify(exactly = 0) { port.cancel(any(), any()) }
        verify { port.resolveRequest(orderId, "REJECTED", any()) }
        verify(exactly = 0) { notifications.record(any(), any(), any()) }
    }

    it("배송 중 주문은 취소할 수 없다") {
        every { port.lock(orderId) } returns order("IN_TRANSIT")
        shouldThrow<IllegalArgumentException> { service.request(customer, orderId) }
    }
})
