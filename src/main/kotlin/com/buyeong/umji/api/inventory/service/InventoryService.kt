package com.buyeong.umji.api.inventory.service

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.inventory.model.MovementPageState
import com.buyeong.umji.api.inventory.model.MovementState
import com.buyeong.umji.api.inventory.model.ReservationState
import com.buyeong.umji.api.inventory.model.SkuReference
import com.buyeong.umji.api.inventory.model.StockState
import com.buyeong.umji.api.inventory.model.StockView
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import com.buyeong.umji.api.persistence.jpa.inventory.entity.InventoryMovementEntity
import com.buyeong.umji.api.persistence.jpa.inventory.entity.InventoryStockEntity
import com.buyeong.umji.api.persistence.jpa.inventory.entity.StockReservationEntity
import com.buyeong.umji.api.persistence.jpa.inventory.service.InventoryJpaEntityService
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InventoryService(
    private val catalog: CatalogJpaEntityService,
    private val inventory: InventoryJpaEntityService,
) {
    @Transactional(readOnly = true)
    fun stock(skuId: UUID, organizationPublicId: UUID? = null): StockView {
        val sku = sku(skuId)
        val stock = inventory.stock(requireNotNull(entitySku(skuId).id), inventory.organizationId(organizationPublicId))?.toState()
        return (stock ?: StockState(sku, 0, 0, 0)).toView(unitsPerSale(skuId, organizationPublicId))
    }

    @Transactional(readOnly = true)
    fun movements(skuId: UUID, page: Int, size: Int, organizationPublicId: UUID? = null): MovementPageState {
        val sku = entitySku(skuId)
        val organizationId = inventory.organizationId(organizationPublicId)
        val pageable = PageRequest.of(page, size, Sort.by("occurredAt").descending())
        val result = if (organizationId == null) inventory.movements(requireNotNull(sku.id), pageable)
        else inventory.movements(requireNotNull(sku.id), organizationId, pageable)
        return MovementPageState(result.content.map { it.toState() }, result.number, result.size, result.totalElements, result.totalPages)
    }

    @Transactional
    fun adjust(skuId: UUID, quantityDelta: Int, reason: String, memo: String?, safetyStock: Int?, organizationPublicId: UUID? = null): StockView {
        require(quantityDelta != 0) { "재고 조정 수량은 0일 수 없습니다." }
        require(safetyStock == null || safetyStock >= 0) { "안전 재고는 0 이상이어야 합니다." }
        val sku = entitySku(skuId)
        val stock = inventory.lockedStock(sku, inventory.organizationId(organizationPublicId))
        stock.onHandQuantity += quantityDelta
        if (safetyStock != null) stock.safetyStockQuantity = safetyStock
        require(stock.onHandQuantity >= stock.reservedQuantity) { "예약 재고보다 실재고를 낮출 수 없습니다." }
        val saved = inventory.saveStock(stock)
        inventory.saveMovement(movement(sku, ADJUSTMENT, quantityDelta, reason.trim(), null, memo, organizationPublicId))
        return saved.toState().toView(unitsPerSale(skuId, organizationPublicId))
    }

    @Transactional
    fun reserve(skuId: UUID, quantity: Int, reservationKey: UUID, expiresAt: Instant?, organizationPublicId: UUID? = null): StockView {
        require(quantity > 0) { "예약 수량은 1 이상이어야 합니다." }
        require(inventory.reservation(reservationKey) == null) { "이미 처리된 재고 예약입니다." }
        val sku = entitySku(skuId)
        val stock = inventory.lockedStock(sku, inventory.organizationId(organizationPublicId))
        require(stock.onHandQuantity - stock.reservedQuantity >= quantity) { "가용 재고가 부족합니다." }
        stock.reservedQuantity += quantity
        val updated = inventory.saveStock(stock)
        inventory.saveReservation(
            StockReservationEntity().apply {
                this.reservationKey = reservationKey
                this.sku = sku
                organization = organizationPublicId?.let(inventory::organization)
                this.quantity = quantity
                status = RESERVED
                this.expiresAt = expiresAt
            },
        )
        inventory.saveMovement(movement(sku, RESERVATION, -quantity, "ORDER_RESERVATION", reservationKey, null, organizationPublicId))
        return updated.toState().toView(unitsPerSale(skuId, organizationPublicId))
    }

    @Transactional
    fun release(reservationKey: UUID): StockView {
        val reservation = reservationEntity(reservationKey)
        require(reservation.status == RESERVED) { "해제할 수 없는 재고 예약입니다." }
        val stock = inventory.lockedStock(reservation.sku, inventory.organizationId(reservation.organization?.publicId))
        stock.reservedQuantity -= reservation.quantity
        val updated = inventory.saveStock(stock)
        reservation.status = RELEASED
        reservation.releasedAt = Instant.now()
        inventory.saveReservation(reservation)
        inventory.saveMovement(movement(reservation.sku, RELEASE, reservation.quantity, "ORDER_RESERVATION", reservationKey, null, reservation.organization?.publicId))
        return updated.toState().toView(unitsPerSale(reservation.sku.publicId ?: error("SKU 식별자가 없습니다."), reservation.organization?.publicId))
    }

    @Transactional
    fun confirm(reservationKey: UUID): StockView {
        val reservation = reservationEntity(reservationKey)
        if (reservation.status == CONFIRMED) return stock(reservation.sku.publicId ?: error("SKU 식별자가 없습니다."), reservation.organization?.publicId)
        require(reservation.status == RESERVED) { "확정할 수 없는 재고 예약입니다." }
        val stock = inventory.lockedStock(reservation.sku, inventory.organizationId(reservation.organization?.publicId))
        stock.onHandQuantity -= reservation.quantity
        stock.reservedQuantity -= reservation.quantity
        val updated = inventory.saveStock(stock)
        reservation.status = CONFIRMED
        inventory.saveReservation(reservation)
        inventory.saveMovement(movement(reservation.sku, CONFIRMATION, -reservation.quantity, "ORDER_RESERVATION", reservationKey, null, reservation.organization?.publicId))
        return updated.toState().toView(unitsPerSale(reservation.sku.publicId ?: error("SKU 식별자가 없습니다."), reservation.organization?.publicId))
    }

    @Transactional
    fun restoreConfirmed(reservationKey: UUID): StockView {
        val reservation = reservationEntity(reservationKey)
        if (reservation.status == RESTORED) return stock(reservation.sku.publicId ?: error("SKU 식별자가 없습니다."), reservation.organization?.publicId)
        require(reservation.status == CONFIRMED) { "확정된 재고만 복구할 수 있습니다." }
        val stock = inventory.lockedStock(reservation.sku, inventory.organizationId(reservation.organization?.publicId))
        stock.onHandQuantity += reservation.quantity
        val updated = inventory.saveStock(stock)
        reservation.status = RESTORED
        reservation.releasedAt = Instant.now()
        inventory.saveReservation(reservation)
        inventory.saveMovement(movement(reservation.sku, RESTOCK, reservation.quantity, "ORDER_CANCELLATION", reservationKey, null, reservation.organization?.publicId))
        return updated.toState().toView(unitsPerSale(reservation.sku.publicId ?: error("SKU 식별자가 없습니다."), reservation.organization?.publicId))
    }

    private fun sku(id: UUID) = entitySku(id).toReference()
    private fun entitySku(id: UUID) = catalog.sku(id) ?: throw ItemNotFoundException("SKU를 찾을 수 없습니다.")
    private fun reservationEntity(key: UUID) = inventory.reservation(key) ?: throw ItemNotFoundException("재고 예약을 찾을 수 없습니다.")
    private fun StockState.toView(unitsPerSale: Int = 1) = StockView(sku.id, sku.code, onHand, reserved, available, safety, unitsPerSale)
    private fun unitsPerSale(skuId: UUID, organizationId: UUID?) =
        (organizationId?.let { catalog.salesOffer("WHOLESALE", skuId, it) } ?: catalog.salesOffer("WHOLESALE", skuId))?.unitsPerSale ?: 1
    private fun ProductSkuEntity.toReference() = SkuReference(requireNotNull(publicId), skuCode)
    private fun InventoryStockEntity.toState() = StockState(sku.toReference(), onHandQuantity, reservedQuantity, safetyStockQuantity)
    private fun StockReservationEntity.toState() = ReservationState(reservationKey, sku.toReference(), quantity, status, expiresAt, releasedAt)
    private fun com.buyeong.umji.api.persistence.jpa.inventory.entity.InventoryMovementEntity.toState() = MovementState(
        requireNotNull(id),
        sku.toReference(),
        movementType,
        quantityDelta,
        referenceType,
        referenceId,
        memo,
        occurredAt,
    )
    private fun movement(sku: ProductSkuEntity, type: String, delta: Int, referenceType: String?, referenceId: UUID?, memo: String?, organizationPublicId: UUID? = null) =
        InventoryMovementEntity().apply {
            this.sku = sku
            organization = organizationPublicId?.let(inventory::organization)
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
