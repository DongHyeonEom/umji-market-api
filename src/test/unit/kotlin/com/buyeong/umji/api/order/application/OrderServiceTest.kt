package com.buyeong.umji.api.order.application

import com.buyeong.umji.api.notification.application.model.NotificationEventType
import com.buyeong.umji.api.notification.application.port.`in`.NotificationEventUseCase
import com.buyeong.umji.api.order.application.model.CheckoutLine
import com.buyeong.umji.api.order.application.model.OrderItemView
import com.buyeong.umji.api.order.application.model.OrderView
import com.buyeong.umji.api.order.application.model.ShippingAddressSnapshot
import com.buyeong.umji.api.order.application.port.out.CheckoutCartPort
import com.buyeong.umji.api.order.application.port.out.InventoryReservationPort
import com.buyeong.umji.api.order.application.port.out.OrderShippingAddressPort
import com.buyeong.umji.api.order.application.port.out.OrderStorePort
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class OrderServiceTest : DescribeSpec({
    val carts = mockk<CheckoutCartPort>()
    val inventory = mockk<InventoryReservationPort>(relaxed = true)
    val orders = mockk<OrderStorePort>()
    val shippingAddresses = mockk<OrderShippingAddressPort>()
    val notifications = mockk<NotificationEventUseCase>(relaxed = true)
    val service = OrderService(carts, inventory, orders, shippingAddresses, notifications = notifications)
    val accountId = UUID.randomUUID()
    val addressId = UUID.randomUUID()
    val skuId = UUID.randomUUID()

    describe("주문 생성") {
        it("가격 스냅샷을 저장하고 재고를 예약한 뒤 장바구니를 비운다") {
            every { shippingAddresses.findForAccount(accountId, addressId) } returns
                ShippingAddressSnapshot("수령인", "01012345678", "12345", "서울 주소", null)
            every { carts.linesForCheckout(accountId) } returns listOf(CheckoutLine(skuId, "SKU-001", "테스트 상품", "규격 A", 12000, 3, "ON_SALE"))
            every { orders.save(any()) } answers {
                val draft = firstArg<com.buyeong.umji.api.order.application.model.OrderDraft>()
                OrderView(
                    UUID.randomUUID(),
                    "UMJ-20260923-000001",
                    draft.status,
                    draft.subtotalAmount,
                    draft.totalAmount,
                    draft.orderedAt,
                    draft.items.map {
                        OrderItemView(UUID.randomUUID(), it.skuId, it.reservationKey, it.productName, it.skuName, it.skuCode, it.unitPrice, it.quantity, it.lineAmount, it.status)
                    },
                )
            }
            every { carts.clear(accountId) } returns Unit

            val result = service.create(accountId, addressId)

            result.status shouldBe "PENDING_PAYMENT"
            result.subtotalAmount shouldBe 36000L
            result.items.single().unitPrice shouldBe 12000L
            result.items.single().quantity shouldBe 3
            verify(exactly = 1) { notifications.record(NotificationEventType.ORDER_CREATED, result.id, null) }
            verify(exactly = 1) { inventory.reserve(skuId, 3, any(), null) }
            verify(exactly = 1) { carts.clear(accountId) }
        }
    }
})