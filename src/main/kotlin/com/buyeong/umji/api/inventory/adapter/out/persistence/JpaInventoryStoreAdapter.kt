package com.buyeong.umji.api.inventory.adapter.out.persistence

import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.inventory.application.model.MovementPageState
import com.buyeong.umji.api.inventory.application.model.MovementState
import com.buyeong.umji.api.inventory.application.model.ReservationState
import com.buyeong.umji.api.inventory.application.model.SkuReference
import com.buyeong.umji.api.inventory.application.model.StockState
import com.buyeong.umji.api.inventory.application.port.out.InventoryStorePort
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.inventory.InventoryJpaEntityService
import com.buyeong.umji.api.persistence.jpa.inventory.InventoryMovementEntity
import com.buyeong.umji.api.persistence.jpa.inventory.InventoryStockEntity
import com.buyeong.umji.api.persistence.jpa.inventory.StockReservationEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class JpaInventoryStoreAdapter(
    private val catalog: CatalogJpaEntityService,
    private val inventory: InventoryJpaEntityService,
) : InventoryStorePort {
    @Transactional(readOnly = true)
    override fun sku(id: UUID): SkuReference? = catalog.sku(id)?.toReference()

    @Transactional(readOnly = true)
    override fun stock(skuId: UUID): StockState? {
        val sku = catalog.sku(skuId) ?: throw ItemNotFoundException("SKU를 찾을 수 없습니다.")
        return inventory.stock(requireNotNull(sku.id))?.toState()
    }

    @Transactional
    override fun lockStock(skuId: UUID): StockState {
        val sku = entitySku(skuId)
        return inventory.lockedStock(sku).toState()
    }

    @Transactional
    override fun saveStock(stock: StockState): StockState {
        val sku = entitySku(stock.sku.id)
        val entity = inventory.lockedStock(sku)
        entity.onHandQuantity = stock.onHand
        entity.reservedQuantity = stock.reserved
        entity.safetyStockQuantity = stock.safety
        return inventory.saveStock(entity).toState()
    }

    @Transactional(readOnly = true)
    override fun reservation(key: UUID): ReservationState? = inventory.reservation(key)?.toState()

    @Transactional
    override fun saveReservation(reservation: ReservationState) {
        val entity = inventory.reservation(reservation.key) ?: StockReservationEntity().apply { reservationKey = reservation.key }
        entity.sku = entitySku(reservation.sku.id)
        entity.quantity = reservation.quantity
        entity.status = reservation.status
        entity.expiresAt = reservation.expiresAt
        entity.releasedAt = reservation.releasedAt
        inventory.saveReservation(entity)
    }

    @Transactional
    override fun saveMovement(sku: SkuReference, type: String, delta: Int, referenceType: String?, referenceId: UUID?, memo: String?) {
        inventory.saveMovement(
            InventoryMovementEntity().apply {
                this.sku = entitySku(sku.id)
                movementType = type
                quantityDelta = delta
                this.referenceType = referenceType
                this.referenceId = referenceId
                this.memo = memo?.trim()?.ifBlank { null }
            },
        )
    }

    @Transactional(readOnly = true)
    override fun movements(skuId: UUID, page: Int, size: Int): MovementPageState {
        val sku = entitySku(skuId)
        val result = inventory.movements(requireNotNull(sku.id), PageRequest.of(page, size, Sort.by("occurredAt").descending()))
        return MovementPageState(result.content.map { it.toState() }, result.number, result.size, result.totalElements, result.totalPages)
    }

    private fun entitySku(id: UUID): ProductSkuEntity = catalog.sku(id) ?: throw ItemNotFoundException("SKU를 찾을 수 없습니다.")
    private fun ProductSkuEntity.toReference() = SkuReference(requireNotNull(publicId), skuCode)
    private fun InventoryStockEntity.toState() = StockState(sku.toReference(), onHandQuantity, reservedQuantity, safetyStockQuantity)
    private fun StockReservationEntity.toState() = ReservationState(reservationKey, sku.toReference(), quantity, status, expiresAt, releasedAt)
    private fun InventoryMovementEntity.toState() = MovementState(
        requireNotNull(id),
        sku.toReference(),
        movementType,
        quantityDelta,
        referenceType,
        referenceId,
        memo,
        occurredAt,
    )
}