package com.buyeong.umji.api.order.adapter

import com.buyeong.umji.api.inventory.application.port.`in`.InventoryUseCase
import com.buyeong.umji.api.order.application.CustomerOrderListingService
import com.buyeong.umji.api.order.application.OrderCancellationService
import com.buyeong.umji.api.order.application.OrderService
import com.buyeong.umji.api.order.application.ShippingHolidayService
import com.buyeong.umji.api.order.application.port.`in`.CustomerOrderListingUseCase
import com.buyeong.umji.api.order.application.port.`in`.OrderUseCase
import com.buyeong.umji.api.order.application.port.out.BankAccountInstructionsPort
import com.buyeong.umji.api.order.application.port.out.CheckoutCartPort
import com.buyeong.umji.api.order.application.port.out.InventoryReservationPort
import com.buyeong.umji.api.order.application.port.out.OrderCancellationPort
import com.buyeong.umji.api.order.application.port.out.OrderStorePort
import com.buyeong.umji.api.order.application.port.out.ShippingHolidayPort
import com.buyeong.umji.api.shipment.application.port.`in`.ShipmentUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OrderApplicationConfiguration {
    @Bean
    fun orderCancellationUseCase(cancellations: OrderCancellationPort, inventory: InventoryUseCase) =
        OrderCancellationService(cancellations, inventory)

    @Bean
    fun shippingHolidayUseCase(holidays: ShippingHolidayPort) = ShippingHolidayService(holidays)

    @Bean
    fun orderService(
        checkoutCart: CheckoutCartPort,
        inventory: InventoryReservationPort,
        orders: OrderStorePort,
        bankAccounts: BankAccountInstructionsPort,
    ) = OrderService(checkoutCart, inventory, orders, bankAccounts)

    @Bean
    fun customerOrderListingUseCase(orders: OrderUseCase, shipments: ShipmentUseCase): CustomerOrderListingUseCase =
        CustomerOrderListingService(orders, shipments)
}