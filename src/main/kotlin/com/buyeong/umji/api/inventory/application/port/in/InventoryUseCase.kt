package com.buyeong.umji.api.inventory.application.port.`in`

import com.buyeong.umji.api.inventory.application.model.MovementPageState
import com.buyeong.umji.api.inventory.application.model.StockView
import java.time.Instant
import java.util.UUID

interface InventoryUseCase {
    fun stock(skuId: UUID): StockView
    fun movements(skuId: UUID, page: Int, size: Int): MovementPageState
    fun adjust(skuId: UUID, quantityDelta: Int, reason: String, memo: String?, safetyStock: Int?): StockView
    fun reserve(skuId: UUID, quantity: Int, reservationKey: UUID, expiresAt: Instant?): StockView
    fun release(reservationKey: UUID): StockView
    fun confirm(reservationKey: UUID): StockView
}