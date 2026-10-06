package com.buyeong.umji.api.order.service

import com.buyeong.umji.api.order.model.OrderPage
import com.buyeong.umji.api.shipment.service.ShipmentService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CustomerOrderListingService(
    private val orders: OrderService,
    private val shipments: ShipmentService,
) {
    @Transactional(readOnly = true)
    fun list(accountPublicId: UUID, page: Int, size: Int): OrderPage {
        shipments.refreshForCustomer(accountPublicId)
        return orders.list(accountPublicId, page, size)
    }
}
