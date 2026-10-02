package com.buyeong.umji.api.payment.application.port.`in`

import com.buyeong.umji.api.payment.application.model.PaymentQueuePage
import com.buyeong.umji.api.payment.application.model.PaymentStatusChange
import java.util.UUID

interface PaymentUseCase {
    fun queue(status: String?, page: Int, size: Int): PaymentQueuePage
    fun updateStatus(orderId: UUID, status: String, operatorId: UUID): PaymentStatusChange
}