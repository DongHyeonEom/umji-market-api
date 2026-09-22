package com.buyeong.umji.api.inventory.service

import com.buyeong.umji.api.persistence.jpa.catalog.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.inventory.model.AdjustInventoryRequest
import com.buyeong.umji.api.inventory.model.InventoryMovementPageResponse
import com.buyeong.umji.api.inventory.model.InventoryMovementResponse
import com.buyeong.umji.api.inventory.model.InventoryStockResponse
import com.buyeong.umji.api.persistence.jpa.inventory.InventoryMovementEntity
import com.buyeong.umji.api.persistence.jpa.inventory.InventoryJpaEntityService
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
    fun stock(skuId: UUID): InventoryStockResponse {
        val sku = sku(skuId)
        return stockResponse(sku, inventory.stock(requireNotNull(sku.id)))
    }

    @Transactional(readOnly = true)
    fun movements(skuId: UUID, page: Int, size: Int): InventoryMovementPageResponse {
        val sku = sku(skuId)
        val result = inventory.movements(requireNotNull(sku.id), PageRequest.of(page, size, Sort.by("occurredAt").descending()))
        return InventoryMovementPageResponse(result.content.map(::movementResponse), result.number, result.size, result.totalElements, result.totalPages)
    }

    @Transactional
    fun adjust(skuId: UUID, request: AdjustInventoryRequest): InventoryStockResponse {
        require(request.quantityDelta != 0) { "재고 조정 수량은 0일 수 없습니다." }
        require(request.safetyStockQuantity == null || request.safetyStockQuantity >= 0) { "안전 재고는 0 이상이어야 합니다." }
        val sku = sku(skuId)
        val stock = lockedStock(sku)
        val nextOnHand = stock.onHandQuantity + request.quantityDelta
        require(nextOnHand >= stock.reservedQuantity) { "예약 재고보다 실재고를 낮출 수 없습니다." }
        stock.onHandQuantity = nextOnHand
        request.safetyStockQuantity?.let { stock.safetyStockQuantity = it }
        inventory.saveMovement(movement(sku, ADJUSTMENT, request.quantityDelta, request.reason.trim(), null, request.memo))
        return stockResponse(sku, stock)
    }

    @Transactional
    fun reserve(skuId: UUID, quantity: Int, reservationKey: UUID, expiresAt: Instant?): InventoryStockResponse {
        require(quantity > 0) { "예약 수량은 1 이상이어야 합니다." }
        require(inventory.reservation(reservationKey) == null) { "이미 처리된 재고 예약입니다." }
        val sku = sku(skuId)
        val stock = lockedStock(sku)
        require(stock.availableQuantity() >= quantity) { "가용 재고가 부족합니다." }
        stock.reservedQuantity += quantity
        inventory.saveReservation(StockReservationEntity().apply {
            this.reservationKey = reservationKey
            this.sku = sku
            this.quantity = quantity
            status = RESERVED
            this.expiresAt = expiresAt
        })
        inventory.saveMovement(movement(sku, RESERVATION, -quantity, "ORDER_RESERVATION", reservationKey, null))
        return stockResponse(sku, stock)
    }

    @Transactional
    fun release(reservationKey: UUID): InventoryStockResponse {
        val reservation = reservation(reservationKey)
        require(reservation.status == RESERVED) { "해제할 수 없는 재고 예약입니다." }
        val stock = lockedStock(reservation.sku)
        stock.reservedQuantity -= reservation.quantity
        reservation.status = RELEASED
        reservation.releasedAt = Instant.now()
        inventory.saveMovement(movement(reservation.sku, RELEASE, reservation.quantity, "ORDER_RESERVATION", reservationKey, null))
        return stockResponse(reservation.sku, stock)
    }

    @Transactional
    fun confirm(reservationKey: UUID): InventoryStockResponse {
        val reservation = reservation(reservationKey)
        require(reservation.status == RESERVED) { "확정할 수 없는 재고 예약입니다." }
        val stock = lockedStock(reservation.sku)
        stock.onHandQuantity -= reservation.quantity
        stock.reservedQuantity -= reservation.quantity
        reservation.status = CONFIRMED
        inventory.saveMovement(movement(reservation.sku, CONFIRMATION, -reservation.quantity, "ORDER_RESERVATION", reservationKey, null))
        return stockResponse(reservation.sku, stock)
    }

    private fun lockedStock(sku: ProductSkuEntity): InventoryStockEntity =
        inventory.lockedStock(sku)

    private fun sku(skuId: UUID): ProductSkuEntity =
        catalog.sku(skuId) ?: throw ItemNotFoundException("SKU를 찾을 수 없습니다.")

    private fun reservation(reservationKey: UUID): StockReservationEntity =
        inventory.reservation(reservationKey) ?: throw ItemNotFoundException("재고 예약을 찾을 수 없습니다.")

    private fun movement(
        sku: ProductSkuEntity,
        type: String,
        quantityDelta: Int,
        referenceType: String?,
        referenceId: UUID?,
        memo: String?,
    ) = InventoryMovementEntity().apply {
        this.sku = sku
        movementType = type
        this.quantityDelta = quantityDelta
        this.referenceType = referenceType
        this.referenceId = referenceId
        this.memo = memo?.trim()?.ifBlank { null }
    }

    private fun stockResponse(sku: ProductSkuEntity, stock: InventoryStockEntity?) = InventoryStockResponse(
        requireNotNull(sku.publicId), sku.skuCode, stock?.onHandQuantity ?: 0, stock?.reservedQuantity ?: 0,
        stock?.availableQuantity() ?: 0, stock?.safetyStockQuantity ?: 0,
    )

    private fun movementResponse(movement: InventoryMovementEntity) = InventoryMovementResponse(
        requireNotNull(movement.id), requireNotNull(movement.sku.publicId), movement.sku.skuCode, movement.movementType,
        movement.quantityDelta, movement.referenceType, movement.referenceId, movement.memo, movement.occurredAt,
    )

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
