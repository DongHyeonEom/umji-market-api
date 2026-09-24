package com.buyeong.umji.api.inventory.adapter.`in`.web

import com.buyeong.umji.api.inventory.application.InventoryService
import com.buyeong.umji.api.inventory.application.model.MovementPageState
import com.buyeong.umji.api.inventory.application.model.StockView
import com.buyeong.umji.api.inventory.application.port.`in`.InventoryUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class TransactionalInventoryUseCase(private val inventory: InventoryService) : InventoryUseCase {
    @Transactional(readOnly = true)
    override fun stock(skuId: UUID): StockView = inventory.stock(skuId)

    @Transactional(readOnly = true)
    override fun movements(skuId: UUID, page: Int, size: Int): MovementPageState = inventory.movements(skuId, page, size)

    @Transactional override fun adjust(skuId: UUID, quantityDelta: Int, reason: String, memo: String?, safetyStock: Int?): StockView =
        inventory.adjust(skuId, quantityDelta, reason, memo, safetyStock)

    @Transactional override fun reserve(skuId: UUID, quantity: Int, reservationKey: UUID, expiresAt: Instant?): StockView =
        inventory.reserve(skuId, quantity, reservationKey, expiresAt)

    @Transactional override fun release(reservationKey: UUID): StockView = inventory.release(reservationKey)

    @Transactional override fun confirm(reservationKey: UUID): StockView = inventory.confirm(reservationKey)
}