package com.buyeong.umji.api.inventory.application.port.out

import com.buyeong.umji.api.inventory.application.model.MovementPageState
import com.buyeong.umji.api.inventory.application.model.ReservationState
import com.buyeong.umji.api.inventory.application.model.SkuReference
import com.buyeong.umji.api.inventory.application.model.StockState
import java.util.UUID

interface InventoryStorePort {
    fun sku(id: UUID): SkuReference?
    fun stock(skuId: UUID): StockState?
    fun lockStock(skuId: UUID): StockState
    fun saveStock(stock: StockState): StockState
    fun reservation(key: UUID): ReservationState?
    fun saveReservation(reservation: ReservationState)
    fun saveMovement(sku: SkuReference, type: String, delta: Int, referenceType: String?, referenceId: UUID?, memo: String?)
    fun movements(skuId: UUID, page: Int, size: Int): MovementPageState
}