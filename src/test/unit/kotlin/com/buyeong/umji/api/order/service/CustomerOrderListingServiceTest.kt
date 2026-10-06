package com.buyeong.umji.api.order.service

import com.buyeong.umji.api.order.model.OrderPage
import com.buyeong.umji.api.order.service.OrderService
import com.buyeong.umji.api.shipment.service.ShipmentService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import java.util.UUID

class CustomerOrderListingServiceTest : DescribeSpec({
    val customerId = UUID.randomUUID()
    val orders = mockk<OrderService>()
    val shipments = mockk<ShipmentService>()
    val service = CustomerOrderListingService(orders, shipments)
    val page = OrderPage(emptyList(), 0, 20, 0, 0)

    it("배송 상태 최신화가 끝난 후 주문 목록을 조회한다") {
        every { shipments.refreshForCustomer(customerId) } returns 1
        every { orders.list(customerId, 0, 20) } returns page

        service.list(customerId, 0, 20) shouldBe page

        verifyOrder {
            shipments.refreshForCustomer(customerId)
            orders.list(customerId, 0, 20)
        }
    }
})