package com.buyeong.umji.api.domain.order.service

import com.buyeong.umji.api.domain.order.dto.OrderPageDto
import com.buyeong.umji.api.domain.shipment.service.ShipmentService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CustomerOrderListingService(
    private val orders: OrderService,
    private val shipments: ShipmentService,
) {
    @Transactional(readOnly = true)
    fun list(accountPublicId: UUID, page: Int, size: Int): OrderPageDto {
        shipments.refreshForCustomer(accountPublicId)
        return orders.list(accountPublicId, page, size)
    }
}