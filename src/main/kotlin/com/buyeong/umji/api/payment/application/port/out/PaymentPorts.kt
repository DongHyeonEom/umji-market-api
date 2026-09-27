package com.buyeong.umji.api.payment.application.port.out

import com.buyeong.umji.api.payment.application.model.PaymentQueuePage
import com.buyeong.umji.api.payment.application.model.PaymentRecord
import com.buyeong.umji.api.payment.application.model.PaymentStatusChange
import java.util.UUID

interface PaymentStorePort {
    fun queue(status: String?, page: Int, size: Int): PaymentQueuePage
    fun lock(orderId: UUID): PaymentRecord?
    fun updateStatus(record: PaymentRecord, status: String, operatorId: UUID): PaymentStatusChange
}

interface PaymentInventoryPort {
    fun confirm(reservationKey: UUID)
}
