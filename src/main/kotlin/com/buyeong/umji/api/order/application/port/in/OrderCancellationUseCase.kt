package com.buyeong.umji.api.order.application.port.`in`

import com.buyeong.umji.api.order.application.model.CancellationChange
import com.buyeong.umji.api.order.application.model.CancellationQueuePage
import java.util.UUID

interface OrderCancellationUseCase {
    fun request(accountId: UUID, orderId: UUID): CancellationChange
    fun queue(page: Int, size: Int): CancellationQueuePage
    fun resolve(orderId: UUID, approved: Boolean, operatorId: UUID): CancellationChange
}