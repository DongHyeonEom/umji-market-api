package com.buyeong.umji.api.operation.payment.adapter.`in`.web

import com.buyeong.umji.api.payment.application.model.PaymentQueuePage
import com.buyeong.umji.api.payment.application.model.PaymentStatusChange
import com.buyeong.umji.api.payment.application.port.`in`.PaymentUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TransactionalPaymentUseCase(private val payments: PaymentUseCase) {
    @Transactional(readOnly = true)
    fun queue(status: String?, page: Int, size: Int): PaymentQueuePage = payments.queue(status, page, size)

    @Transactional
    fun updateStatus(orderId: UUID, status: String, operatorId: UUID): PaymentStatusChange =
        payments.updateStatus(orderId, status, operatorId)
}