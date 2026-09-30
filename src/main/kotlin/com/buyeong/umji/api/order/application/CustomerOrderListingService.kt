package com.buyeong.umji.api.order.application

import com.buyeong.umji.api.order.application.model.OrderPage
import com.buyeong.umji.api.order.application.port.`in`.CustomerOrderListingUseCase
import com.buyeong.umji.api.order.application.port.`in`.OrderUseCase
import com.buyeong.umji.api.shipment.application.port.`in`.ShipmentUseCase
import java.util.UUID

class CustomerOrderListingService(
    private val orders: OrderUseCase,
    private val shipments: ShipmentUseCase,
) : CustomerOrderListingUseCase {
    override fun list(accountPublicId: UUID, page: Int, size: Int): OrderPage {
        shipments.refreshForCustomer(accountPublicId)
        return orders.list(accountPublicId, page, size)
    }
}