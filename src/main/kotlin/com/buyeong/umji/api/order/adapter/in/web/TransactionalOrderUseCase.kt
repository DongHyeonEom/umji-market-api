package com.buyeong.umji.api.order.adapter.`in`.web

import com.buyeong.umji.api.order.application.model.OrderPage
import com.buyeong.umji.api.order.application.model.OrderView
import com.buyeong.umji.api.order.application.port.`in`.OrderUseCase
import com.buyeong.umji.api.order.application.OrderService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TransactionalOrderUseCase(private val orders: OrderService) : OrderUseCase {
    @Transactional
    override fun create(accountPublicId: UUID): OrderView = orders.create(accountPublicId)

    @Transactional(readOnly = true)
    override fun list(accountPublicId: UUID, page: Int, size: Int): OrderPage = orders.list(accountPublicId, page, size)

    @Transactional(readOnly = true)
    override fun detail(accountPublicId: UUID, orderId: UUID): OrderView = orders.detail(accountPublicId, orderId)
}
