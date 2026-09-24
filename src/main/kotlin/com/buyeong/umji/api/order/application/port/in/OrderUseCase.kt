package com.buyeong.umji.api.order.application.port.`in`

import com.buyeong.umji.api.order.application.model.OrderPage
import com.buyeong.umji.api.order.application.model.OrderView
import java.util.UUID

interface OrderUseCase {
    fun create(accountPublicId: UUID): OrderView
    fun list(accountPublicId: UUID, page: Int, size: Int): OrderPage
    fun detail(accountPublicId: UUID, orderId: UUID): OrderView
}