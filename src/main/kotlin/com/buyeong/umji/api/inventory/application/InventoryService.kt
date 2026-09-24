package com.buyeong.umji.api.inventory.application

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.inventory.application.model.MovementPageState
import com.buyeong.umji.api.inventory.application.model.ReservationState
import com.buyeong.umji.api.inventory.application.model.StockState
import com.buyeong.umji.api.inventory.application.model.StockView
import com.buyeong.umji.api.inventory.application.port.`in`.InventoryUseCase
import com.buyeong.umji.api.inventory.application.port.out.InventoryStorePort
import java.time.Instant
import java.util.UUID

class InventoryService(private val store: InventoryStorePort) : InventoryUseCase {
    override fun stock(skuId: UUID): StockView {
        val sku = sku(skuId)
        return store.stock(skuId)?.toView() ?: StockState(sku, 0, 0, 0).toView()
    }

    override fun movements(skuId: UUID, page: Int, size: Int): MovementPageState {
        sku(skuId)
        return store.movements(skuId, page, size)
    }

    override fun adjust(skuId: UUID, quantityDelta: Int, reason: String, memo: String?, safetyStock: Int?): StockView {
        require(quantityDelta != 0) { "재고 조정 수량은 0일 수 없습니다." }
        require(safetyStock == null || safetyStock >= 0) { "안전 재고는 0 이상이어야 합니다." }
        val stock = store.lockStock(skuId)
        val next = stock.copy(onHand = stock.onHand + quantityDelta, safety = safetyStock ?: stock.safety)
        require(next.onHand >= next.reserved) { "예약 재고보다 실재고를 낮출 수 없습니다." }
        store.saveStock(next)
        store.saveMovement(stock.sku, ADJUSTMENT, quantityDelta, reason.trim(), null, memo)
        return next.toView()
    }

    override fun reserve(skuId: UUID, quantity: Int, reservationKey: UUID, expiresAt: Instant?): StockView {
        require(quantity > 0) { "예약 수량은 1 이상이어야 합니다." }
        require(store.reservation(reservationKey) == null) { "이미 처리된 재고 예약입니다." }
        val stock = store.lockStock(skuId)
        require(stock.available >= quantity) { "가용 재고가 부족합니다." }
        val updated = stock.copy(reserved = stock.reserved + quantity)
        store.saveStock(updated)
        store.saveReservation(ReservationState(reservationKey, stock.sku, quantity, RESERVED, expiresAt))
        store.saveMovement(stock.sku, RESERVATION, -quantity, "ORDER_RESERVATION", reservationKey, null)
        return updated.toView()
    }

    override fun release(reservationKey: UUID): StockView {
        val reservation = reservation(reservationKey)
        require(reservation.status == RESERVED) { "해제할 수 없는 재고 예약입니다." }
        val stock = store.lockStock(reservation.sku.id)
        val updated = stock.copy(reserved = stock.reserved - reservation.quantity)
        store.saveStock(updated)
        store.saveReservation(reservation.copy(status = RELEASED, releasedAt = Instant.now()))
        store.saveMovement(reservation.sku, RELEASE, reservation.quantity, "ORDER_RESERVATION", reservationKey, null)
        return updated.toView()
    }

    override fun confirm(reservationKey: UUID): StockView {
        val reservation = reservation(reservationKey)
        require(reservation.status == RESERVED) { "확정할 수 없는 재고 예약입니다." }
        val stock = store.lockStock(reservation.sku.id)
        val updated = stock.copy(onHand = stock.onHand - reservation.quantity, reserved = stock.reserved - reservation.quantity)
        store.saveStock(updated)
        store.saveReservation(reservation.copy(status = CONFIRMED))
        store.saveMovement(reservation.sku, CONFIRMATION, -reservation.quantity, "ORDER_RESERVATION", reservationKey, null)
        return updated.toView()
    }

    private fun sku(id: UUID) = store.sku(id) ?: throw ItemNotFoundException("SKU를 찾을 수 없습니다.")
    private fun reservation(key: UUID) = store.reservation(key) ?: throw ItemNotFoundException("재고 예약을 찾을 수 없습니다.")
    private fun StockState.toView() = StockView(sku.id, sku.code, onHand, reserved, available, safety)

    private companion object {
        const val ADJUSTMENT = "ADJUSTMENT"
        const val RESERVATION = "RESERVATION"
        const val RELEASE = "RELEASE"
        const val CONFIRMATION = "CONFIRMATION"
        const val RESERVED = "RESERVED"
        const val RELEASED = "RELEASED"
        const val CONFIRMED = "CONFIRMED"
    }
}