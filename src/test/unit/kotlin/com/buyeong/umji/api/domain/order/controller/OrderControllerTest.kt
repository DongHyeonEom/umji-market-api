package com.buyeong.umji.api.domain.order.controller

import com.buyeong.umji.api.domain.auth.service.CurrentAccountService
import com.buyeong.umji.api.domain.order.model.OrderPage
import com.buyeong.umji.api.domain.order.service.CustomerOrderListingService
import com.buyeong.umji.api.domain.order.service.OrderService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import java.util.UUID

class OrderControllerTest : DescribeSpec({
    val customerId = UUID.randomUUID()
    val currentAccounts = mockk<CurrentAccountService>()
    val orders = mockk<OrderService>()
    val customerOrders = mockk<CustomerOrderListingService>()
    val controller = OrderController(currentAccounts, orders, customerOrders)

    it("주문 목록 응답 전에 고객의 배송 상태를 최신화한다") {
        every { currentAccounts.activeAccountPublicId() } returns customerId
        every { customerOrders.list(customerId, 0, 20) } returns OrderPage(emptyList(), 0, 20, 0, 0)

        val result = controller.list(0, 20)

        result.items shouldBe emptyList()
        verifyOrder {
            currentAccounts.activeAccountPublicId()
            customerOrders.list(customerId, 0, 20)
        }
    }
})