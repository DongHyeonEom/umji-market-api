package com.buyeong.umji.api.order.adapter.`in`.web

import com.buyeong.umji.api.order.application.model.CancellationChange
import com.buyeong.umji.api.order.application.model.CancellationQueuePage
import com.buyeong.umji.api.order.application.port.`in`.OrderCancellationUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TransactionalOrderCancellationUseCase(private val cancellations: OrderCancellationUseCase) {
    @Transactional fun request(accountId: UUID, orderId: UUID): CancellationChange = cancellations.request(accountId, orderId)

    @Transactional(readOnly = true)
    fun queue(page: Int, size: Int): CancellationQueuePage = cancellations.queue(page, size)

    @Transactional fun resolve(orderId: UUID, approved: Boolean, operatorId: UUID): CancellationChange =
        cancellations.resolve(orderId, approved, operatorId)
}