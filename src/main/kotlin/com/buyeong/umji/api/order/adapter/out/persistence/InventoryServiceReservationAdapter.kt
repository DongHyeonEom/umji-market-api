package com.buyeong.umji.api.order.adapter.out.persistence

import com.buyeong.umji.api.inventory.application.port.`in`.InventoryUseCase
import com.buyeong.umji.api.order.application.port.out.InventoryReservationPort
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class InventoryServiceReservationAdapter(private val inventory: InventoryUseCase) : InventoryReservationPort {
    override fun reserve(skuId: UUID, quantity: Int, reservationKey: UUID, expiresAt: Instant?) {
        inventory.reserve(skuId, quantity, reservationKey, expiresAt)
    }
}
