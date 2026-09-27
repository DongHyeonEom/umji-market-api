package com.buyeong.umji.api.payment.adapter.out.persistence

import com.buyeong.umji.api.inventory.application.port.`in`.InventoryUseCase
import com.buyeong.umji.api.payment.application.port.out.PaymentInventoryPort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class InventoryPaymentAdapter(private val inventory: InventoryUseCase) : PaymentInventoryPort {
    override fun confirm(reservationKey: UUID) {
        inventory.confirm(reservationKey)
    }
}
