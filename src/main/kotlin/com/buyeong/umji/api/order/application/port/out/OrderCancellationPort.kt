package com.buyeong.umji.api.order.application.port.out

import com.buyeong.umji.api.order.application.model.CancellationOrder
import com.buyeong.umji.api.order.application.model.CancellationQueuePage
import java.util.UUID

interface OrderCancellationPort {
    fun lock(orderId: UUID): CancellationOrder?
    fun hasPendingRequest(orderId: UUID): Boolean
    fun recordRequest(orderId: UUID, requesterId: UUID, status: String, processorId: UUID? = null)
    fun cancel(order: CancellationOrder, processorId: UUID?)
    fun resolveRequest(orderId: UUID, status: String, processorId: UUID)
    fun queue(page: Int, size: Int): CancellationQueuePage
}