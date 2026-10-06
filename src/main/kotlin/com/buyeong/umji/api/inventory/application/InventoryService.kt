package com.buyeong.umji.api.inventory.application

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.inventory.application.model.MovementPageState
import com.buyeong.umji.api.inventory.application.model.ReservationState
import com.buyeong.umji.api.inventory.application.model.MovementState
import com.buyeong.umji.api.inventory.application.model.SkuReference
import com.buyeong.umji.api.inventory.application.model.StockState
import com.buyeong.umji.api.inventory.application.model.StockView
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.inventory.InventoryJpaEntityService
import com.buyeong.umji.api.persistence.jpa.inventory.InventoryMovementEntity
import com.buyeong.umji.api.persistence.jpa.inventory.InventoryStockEntity
import com.buyeong.umji.api.persistence.jpa.inventory.StockReservationEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class InventoryService(
    private val catalog: CatalogJpaEntityService,
    private val inventory: InventoryJpaEntityService,
) {
    @Transactional(readOnly = true)
    fun stock(skuId: UUID): StockView {
        val sku = sku(skuId)
        val stock = inventory.stock(requireNotNull(entitySku(skuId).id))?.toState()
        return (stock ?: StockState(sku, 0, 0, 0)).toView()
    }

    @Transactional(readOnly = true)
    fun movements(skuId: UUID, page: Int, size: Int): MovementPageState {
        val sku = entitySku(skuId)
        val result = inventory.movements(
            requireNotNull(sku.id),
            PageRequest.of(page, size, Sort.by("occurredAt").descending()),
        )
        return MovementPageState(result.content.map { it.toState() }, result.number, result.size, result.totalElements, result.totalPages)
    }

    @Transactional
    fun adjust(skuId: UUID, quantityDelta: Int, reason: String, memo: String?, safetyStock: Int?): StockView {
        require(quantityDelta != 0) { "재고 조정 수량은 0일 수 없습니다." }
        require(safetyStock == null || safetyStock >= 0) { "안전 재고는 0 이상이어야 합니다." }
        val sku = entitySku(skuId)
        val stock = inventory.lockedStock(sku)
        stock.onHandQuantity += quantityDelta
        if (safetyStock != null) stock.safetyStockQuantity = safetyStock
        require(stock.onHandQuantity >= stock.reservedQuantity) { "예약 재고보다 실재고를 낮출 수 없습니다." }
        val saved = inventory.saveStock(stock)
        inventory.saveMovement(movement(sku, ADJUSTMENT, quantityDelta, reason.trim(), null, memo))
        return saved.toState().toView()
    }

    @Transactional
    fun reserve(skuId: UUID, quantity: Int, reservationKey: UUID, expiresAt: Instant?): StockView {
        require(quantity > 0) { "예약 수량은 1 이상이어야 합니다." }
        require(inventory.reservation(reservationKey) == null) { "이미 처리된 재고 예약입니다." }
        val sku = entitySku(skuId)
        val stock = inventory.lockedStock(sku)
        require(stock.onHandQuantity - stock.reservedQuantity >= quantity) { "가용 재고가 부족합니다." }
        stock.reservedQuantity += quantity
        val updated = inventory.saveStock(stock)
        inventory.saveReservation(StockReservationEntity().apply {
            this.reservationKey = reservationKey
            this.sku = sku
            this.quantity = quantity
            status = RESERVED
            this.expiresAt = expiresAt
        })
        inventory.saveMovement(movement(sku, RESERVATION, -quantity, "ORDER_RESERVATION", reservationKey, null))
        return updated.toState().toView()
    }

    @Transactional
    fun release(reservationKey: UUID): StockView {
        val reservation = reservationEntity(reservationKey)
        require(reservation.status == RESERVED) { "해제할 수 없는 재고 예약입니다." }
        val stock = inventory.lockedStock(reservation.sku)
        stock.reservedQuantity -= reservation.quantity
        val updated = inventory.saveStock(stock)
        reservation.status = RELEASED
        reservation.releasedAt = Instant.now()
        inventory.saveReservation(reservation)
        inventory.saveMovement(movement(reservation.sku, RELEASE, reservation.quantity, "ORDER_RESERVATION", reservationKey, null))
        return updated.toState().toView()
    }

    @Transactional
    fun confirm(reservationKey: UUID): StockView {
        val reservation = reservationEntity(reservationKey)
        if (reservation.status == CONFIRMED) return stock(reservation.sku.publicId ?: error("SKU 식별자가 없습니다."))
        require(reservation.status == RESERVED) { "확정할 수 없는 재고 예약입니다." }
        val stock = inventory.lockedStock(reservation.sku)
        stock.onHandQuantity -= reservation.quantity
        stock.reservedQuantity -= reservation.quantity
        val updated = inventory.saveStock(stock)
        reservation.status = CONFIRMED
        inventory.saveReservation(reservation)
        inventory.saveMovement(movement(reservation.sku, CONFIRMATION, -reservation.quantity, "ORDER_RESERVATION", reservationKey, null))
        return updated.toState().toView()
    }

    @Transactional
    fun restoreConfirmed(reservationKey: UUID): StockView {
        val reservation = reservationEntity(reservationKey)
        if (reservation.status == RESTORED) return stock(reservation.sku.publicId ?: error("SKU 식별자가 없습니다."))
        require(reservation.status == CONFIRMED) { "확정된 재고만 복구할 수 있습니다." }
        val stock = inventory.lockedStock(reservation.sku)
        stock.onHandQuantity += reservation.quantity
        val updated = inventory.saveStock(stock)
        reservation.status = RESTORED
        reservation.releasedAt = Instant.now()
        inventory.saveReservation(reservation)
        inventory.saveMovement(movement(reservation.sku, RESTOCK, reservation.quantity, "ORDER_CANCELLATION", reservationKey, null))
        return updated.toState().toView()
    }

    private fun sku(id: UUID) = entitySku(id).toReference()
    private fun entitySku(id: UUID) = catalog.sku(id) ?: throw ItemNotFoundException("SKU를 찾을 수 없습니다.")
    private fun reservationEntity(key: UUID) = inventory.reservation(key) ?: throw ItemNotFoundException("재고 예약을 찾을 수 없습니다.")
    private fun StockState.toView() = StockView(sku.id, sku.code, onHand, reserved, available, safety)
    private fun ProductSkuEntity.toReference() = SkuReference(requireNotNull(publicId), skuCode)
    private fun InventoryStockEntity.toState() = StockState(sku.toReference(), onHandQuantity, reservedQuantity, safetyStockQuantity)
    private fun StockReservationEntity.toState() = ReservationState(reservationKey, sku.toReference(), quantity, status, expiresAt, releasedAt)
    private fun com.buyeong.umji.api.persistence.jpa.inventory.InventoryMovementEntity.toState() = MovementState(
        requireNotNull(id), sku.toReference(), movementType, quantityDelta, referenceType, referenceId, memo, occurredAt,
    )
    private fun movement(sku: ProductSkuEntity, type: String, delta: Int, referenceType: String?, referenceId: UUID?, memo: String?) =
        InventoryMovementEntity().apply {
            this.sku = sku
            movementType = type
            quantityDelta = delta
            this.referenceType = referenceType
            this.referenceId = referenceId
            this.memo = memo?.trim()?.ifBlank { null }
        }

    private companion object {
        const val ADJUSTMENT = "ADJUSTMENT"
        const val RESERVATION = "RESERVATION"
        const val RELEASE = "RELEASE"
        const val CONFIRMATION = "CONFIRMATION"
        const val RESERVED = "RESERVED"
        const val RELEASED = "RELEASED"
        const val CONFIRMED = "CONFIRMED"
        const val RESTORED = "RESTORED"
        const val RESTOCK = "RESTOCK"
    }
}
