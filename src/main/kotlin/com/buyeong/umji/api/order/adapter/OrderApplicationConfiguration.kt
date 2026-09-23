package com.buyeong.umji.api.order.adapter

import com.buyeong.umji.api.order.application.port.out.CheckoutCartPort
import com.buyeong.umji.api.order.application.port.out.InventoryReservationPort
import com.buyeong.umji.api.order.application.port.out.OrderStorePort
import com.buyeong.umji.api.order.application.OrderService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OrderApplicationConfiguration {
    @Bean
    fun orderService(
        checkoutCart: CheckoutCartPort,
        inventory: InventoryReservationPort,
        orders: OrderStorePort,
    ) = OrderService(checkoutCart, inventory, orders)
}
