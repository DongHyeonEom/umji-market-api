package com.buyeong.umji.api.order.application

import com.buyeong.umji.api.notification.application.model.NotificationEventType
import com.buyeong.umji.api.notification.application.NotificationEventService
import com.buyeong.umji.api.order.application.model.CheckoutLine
import com.buyeong.umji.api.cart.application.CartService
import com.buyeong.umji.api.cart.application.model.CartItemView
import com.buyeong.umji.api.cart.application.model.CartView
import com.buyeong.umji.api.inventory.application.InventoryService
import com.buyeong.umji.api.order.application.model.OrderItemView
import com.buyeong.umji.api.order.application.model.OrderView
import com.buyeong.umji.api.order.application.model.ShippingAddressSnapshot
import com.buyeong.umji.api.persistence.jpa.account.CustomerAccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.order.OrderCheckoutJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupTaxInvoiceJpaEntityService
import com.buyeong.umji.api.payment.adapter.TaxInvoiceSupplierAdapter
import com.buyeong.umji.api.payment.adapter.BankAccountInstructionsService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class OrderServiceTest : DescribeSpec({
    val carts = mockk<CartService>()
    val inventory = mockk<InventoryService>(relaxed = true)
    val orders = mockk<OrderCheckoutJpaEntityService>()
    val shippingAddresses = mockk<CustomerAccountJpaEntityService>()
    val notifications = mockk<NotificationEventService>(relaxed = true)
    val taxInvoiceSuppliers = mockk<TaxInvoiceSupplierAdapter>(relaxed = true)
    val taxInvoiceBuyers = mockk<BuyerGroupTaxInvoiceJpaEntityService>(relaxed = true)
    val bankAccounts = mockk<BankAccountInstructionsService>(relaxed = true)
    val service = OrderService(carts, inventory, orders, shippingAddresses, bankAccounts, notifications, taxInvoiceSuppliers, taxInvoiceBuyers)
    val accountId = UUID.randomUUID()
    val addressId = UUID.randomUUID()
    val skuId = UUID.randomUUID()
    val offerId = UUID.randomUUID()

    describe("주문 생성") {
        it("가격 스냅샷을 저장하고 재고를 예약한 뒤 장바구니를 비운다") {
            every { shippingAddresses.findForAccount(accountId, addressId) } returns
                ShippingAddressSnapshot("수령인", "01012345678", "12345", "서울 주소", null)
            every { carts.cart(accountId) } returns CartView(listOf(CartItemView(UUID.randomUUID(), skuId, "SKU-001", "테스트 상품", "규격 A", 3, 12000, "ON_SALE", offerId, "RETAIL")))
            every { orders.save(any()) } answers {
                val draft = firstArg<com.buyeong.umji.api.order.application.model.OrderDraft>()
                draft.channelCode shouldBe "RETAIL"
                draft.items.single().salesOfferId shouldBe offerId
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
            every { carts.clearForCheckout(accountId) } returns Unit

            val result = service.create(accountId, addressId)

            result.status shouldBe "PENDING_PAYMENT"
            result.subtotalAmount shouldBe 36000L
            result.items.single().unitPrice shouldBe 12000L
            result.items.single().quantity shouldBe 3
            verify(exactly = 1) { notifications.record(NotificationEventType.ORDER_CREATED, result.id, null) }
            verify(exactly = 1) { inventory.reserve(skuId, 3, any(), null) }
            verify(exactly = 1) { carts.clearForCheckout(accountId) }
        }
    }
})
