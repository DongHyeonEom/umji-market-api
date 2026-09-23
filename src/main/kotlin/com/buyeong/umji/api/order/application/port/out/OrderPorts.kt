package com.buyeong.umji.api.order.application.port.out

import com.buyeong.umji.api.order.application.model.CheckoutLine
import com.buyeong.umji.api.order.application.model.OrderDraft
import com.buyeong.umji.api.order.application.model.OrderPage
import com.buyeong.umji.api.order.application.model.OrderView
import java.time.Instant
import java.util.UUID

interface CheckoutCartPort {
    fun linesForCheckout(accountId: UUID): List<CheckoutLine>
    fun clear(accountId: UUID)
}

interface InventoryReservationPort {
    fun reserve(skuId: UUID, quantity: Int, reservationKey: UUID, expiresAt: Instant?)
}

interface OrderStorePort {
    fun save(draft: OrderDraft): OrderView
    fun findAll(accountId: UUID, page: Int, size: Int): OrderPage
    fun find(accountId: UUID, orderId: UUID): OrderView?
}
