package com.buyeong.umji.api.domain.inventory.service

import com.buyeong.umji.api.domain.inventory.dto.MovementPageStateDto
import com.buyeong.umji.api.domain.inventory.dto.MovementStateDto
import com.buyeong.umji.api.domain.inventory.dto.ReservationStateDto
import com.buyeong.umji.api.domain.inventory.dto.SkuReferenceDto
import com.buyeong.umji.api.domain.inventory.dto.StockStateDto
import com.buyeong.umji.api.domain.inventory.dto.StockViewDto
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import com.buyeong.umji.api.persistence.jpa.inventory.entity.InventoryMovementEntity
import com.buyeong.umji.api.persistence.jpa.inventory.entity.InventoryStockEntity
import com.buyeong.umji.api.persistence.jpa.inventory.entity.StockReservationEntity
import com.buyeong.umji.api.persistence.jpa.inventory.entity.InventoryCostLotEntity
import com.buyeong.umji.api.persistence.jpa.inventory.entity.StockReservationLotAllocationEntity
import com.buyeong.umji.api.persistence.jpa.inventory.service.InventoryJpaEntityService
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
    @Transactional
    fun registerCostLot(skuId: UUID, organizationPublicId: UUID, reference: String, quantity: Int, totalCost: Long): InventoryCostLotEntity {
        val sku = entitySku(skuId)
        val organizationId = inventory.organizationId(organizationPublicId) ?: throw ItemNotFoundException("판매 Organization을 찾을 수 없습니다.")
        require(sku.product.organization?.id == organizationId) { "SKU와 판매 Organization이 일치하지 않습니다." }
        val stock = inventory.lockedStock(sku, organizationId)
        stock.onHandQuantity = Math.addExact(stock.onHandQuantity, quantity)
        inventory.saveStock(stock)
        val lot = inventory.saveLot(InventoryCostLotEntity().apply {
            this.sku = sku
            organization = inventory.organization(organizationPublicId)
            containerReference = reference.trim()
            receivedQuantity = quantity
            remainingQuantity = quantity
            totalPurchaseCost = totalCost
            remainingCostAmount = totalCost
        })
        inventory.saveMovement(movement(sku, "RECEIPT", quantity, "COST_LOT", null, reference, organizationPublicId))
        return lot
    }

    @Transactional(readOnly = true)
    fun costLots(skuId: UUID, organizationPublicId: UUID?): List<InventoryCostLotEntity> {
        val sku = entitySku(skuId)
        return inventory.lots(requireNotNull(sku.id), inventory.organizationId(organizationPublicId))
    }

    @Transactional
    fun setCostLot(lotId: Long, totalCost: Long): InventoryCostLotEntity {
        val lot = inventory.lockedLot(lotId) ?: throw ItemNotFoundException("재고 원가 lot을 찾을 수 없습니다.")
        require(lot.totalPurchaseCost == null) { "원가가 등록된 lot은 변경할 수 없습니다." }
        require(lot.remainingQuantity == lot.receivedQuantity) { "이미 일부 출고된 lot의 원가는 등록할 수 없습니다." }
        lot.totalPurchaseCost = totalCost
        lot.remainingCostAmount = totalCost
        return inventory.saveLot(lot)
    }

    @Transactional(readOnly = true)
    fun stock(skuId: UUID, organizationPublicId: UUID? = null): StockViewDto {
        val sku = sku(skuId)
        val stock = inventory.stock(requireNotNull(entitySku(skuId).id), inventory.organizationId(organizationPublicId))?.toState()
        return (stock ?: StockStateDto(sku, 0, 0, 0)).toView(unitsPerSale(skuId, organizationPublicId))
    }

    @Transactional(readOnly = true)
    fun movements(skuId: UUID, page: Int, size: Int, organizationPublicId: UUID? = null): MovementPageStateDto {
        val sku = entitySku(skuId)
        val organizationId = inventory.organizationId(organizationPublicId)
        val pageable = PageRequest.of(page, size, Sort.by("occurredAt").descending())
        val result = if (organizationId == null) {
            inventory.movements(requireNotNull(sku.id), pageable)
        } else {
            inventory.movements(requireNotNull(sku.id), organizationId, pageable)
        }
        return MovementPageStateDto(result.content.map { it.toState() }, result.number, result.size, result.totalElements, result.totalPages)
    }

    @Transactional
    fun adjust(skuId: UUID, quantityDelta: Int, reason: String, memo: String?, safetyStock: Int?, organizationPublicId: UUID? = null): StockViewDto {
        require(quantityDelta != 0) { "재고 조정 수량은 0일 수 없습니다." }
        val sku = entitySku(skuId)
        val stock = inventory.lockedStock(sku, inventory.organizationId(organizationPublicId))
        stock.onHandQuantity += quantityDelta
        if (safetyStock != null) stock.safetyStockQuantity = safetyStock
        require(stock.onHandQuantity >= stock.reservedQuantity) { "예약 재고보다 실재고를 낮출 수 없습니다." }
        val saved = inventory.saveStock(stock)
        if (quantityDelta > 0) {
            inventory.saveLot(InventoryCostLotEntity().apply {
                this.sku = sku
                organization = organizationPublicId?.let(inventory::organization)
                containerReference = "UNPRICED-ADJUSTMENT-${UUID.randomUUID()}"
                receivedQuantity = quantityDelta
                remainingQuantity = quantityDelta
            })
        } else {
            consumeUnreservedLots(sku, inventory.organizationId(organizationPublicId), -quantityDelta)
        }
        inventory.saveMovement(movement(sku, ADJUSTMENT, quantityDelta, reason.trim(), null, memo, organizationPublicId))
        return saved.toState().toView(unitsPerSale(skuId, organizationPublicId))
    }

    @Transactional
    fun reserve(skuId: UUID, quantity: Int, reservationKey: UUID, expiresAt: Instant?, organizationPublicId: UUID? = null): StockViewDto {
        require(quantity > 0) { "예약 수량은 1 이상이어야 합니다." }
        require(inventory.reservation(reservationKey) == null) { "이미 처리된 재고 예약입니다." }
        val sku = entitySku(skuId)
        val stock = inventory.lockedStock(sku, inventory.organizationId(organizationPublicId))
        require(stock.onHandQuantity - stock.reservedQuantity >= quantity) { "가용 재고가 부족합니다." }
        stock.reservedQuantity += quantity
        val updated = inventory.saveStock(stock)
        val reservation = inventory.saveReservation(
            StockReservationEntity().apply {
                this.reservationKey = reservationKey
                this.sku = sku
                organization = organizationPublicId?.let(inventory::organization)
                this.quantity = quantity
                status = RESERVED
                this.expiresAt = expiresAt
            },
        )
        var toAllocate = quantity
        inventory.lockedLots(requireNotNull(sku.id), inventory.organizationId(organizationPublicId)).forEach { lot ->
            val available = lot.remainingQuantity - lot.reservedQuantity
            val allocated = minOf(toAllocate, available)
            if (allocated > 0) {
                lot.reservedQuantity += allocated
                inventory.saveLot(lot)
                inventory.saveAllocation(StockReservationLotAllocationEntity().apply {
                    this.reservation = reservation
                    this.lot = lot
                    this.quantity = allocated
                })
                toAllocate -= allocated
            }
        }
        check(toAllocate == 0) { "재고 원가 lot 수량이 재고 원장과 일치하지 않습니다." }
        inventory.saveMovement(movement(sku, RESERVATION, -quantity, "ORDER_RESERVATION", reservationKey, null, organizationPublicId))
        return updated.toState().toView(unitsPerSale(skuId, organizationPublicId))
    }

    @Transactional
    fun release(reservationKey: UUID): StockViewDto {
        val reservation = reservationEntity(reservationKey)
        require(reservation.status == RESERVED) { "해제할 수 없는 재고 예약입니다." }
        val stock = inventory.lockedStock(reservation.sku, inventory.organizationId(reservation.organization?.publicId))
        stock.reservedQuantity -= reservation.quantity
        inventory.allocations(requireNotNull(reservation.id)).filter { it.status == ALLOCATION_RESERVED }.forEach { allocation ->
            allocation.lot.reservedQuantity -= allocation.quantity
            allocation.status = ALLOCATION_RELEASED
            inventory.saveLot(allocation.lot)
            inventory.saveAllocation(allocation)
        }
        val updated = inventory.saveStock(stock)
        reservation.status = RELEASED
        reservation.releasedAt = Instant.now()
        inventory.saveReservation(reservation)
        inventory.saveMovement(movement(reservation.sku, RELEASE, reservation.quantity, "ORDER_RESERVATION", reservationKey, null, reservation.organization?.publicId))
        return updated.toState().toView(unitsPerSale(reservation.sku.publicId ?: error("SKU 식별자가 없습니다."), reservation.organization?.publicId))
    }

    @Transactional
    fun confirm(reservationKey: UUID): StockViewDto {
        confirmCost(reservationKey)
        val reservation = reservationEntity(reservationKey)
        return stock(reservation.sku.publicId ?: error("SKU 식별자가 없습니다."), reservation.organization?.publicId)
    }

    @Transactional
    fun confirmCost(reservationKey: UUID): Long {
        val reservation = reservationEntity(reservationKey)
        if (reservation.status == CONFIRMED) return inventory.allocations(requireNotNull(reservation.id)).sumOf { it.costAmount ?: 0L }
        require(reservation.status == RESERVED) { "확정할 수 없는 재고 예약입니다." }
        val allocations = inventory.allocations(requireNotNull(reservation.id))
        require(allocations.isNotEmpty() && allocations.all { it.lot.totalPurchaseCost != null }) { "미확정 사입원가 lot이 포함되어 출고할 수 없습니다." }
        val stock = inventory.lockedStock(reservation.sku, inventory.organizationId(reservation.organization?.publicId))
        var purchaseCost = 0L
        allocations.forEach { allocation ->
            val lot = allocation.lot
            require(lot.remainingQuantity >= allocation.quantity && lot.reservedQuantity >= allocation.quantity) { "원가 lot 수량이 부족합니다." }
            val remainingCost = requireNotNull(lot.remainingCostAmount)
            val consumedCost = if (allocation.quantity == lot.remainingQuantity) remainingCost else Math.multiplyExact(remainingCost, allocation.quantity.toLong()) / lot.remainingQuantity
            allocation.costAmount = consumedCost
            allocation.status = ALLOCATION_CONFIRMED
            lot.remainingCostAmount = remainingCost - consumedCost
            lot.remainingQuantity -= allocation.quantity
            lot.reservedQuantity -= allocation.quantity
            inventory.saveLot(lot)
            inventory.saveAllocation(allocation)
            purchaseCost = Math.addExact(purchaseCost, consumedCost)
        }
        stock.onHandQuantity -= reservation.quantity
        stock.reservedQuantity -= reservation.quantity
        inventory.saveStock(stock)
        reservation.status = CONFIRMED
        inventory.saveReservation(reservation)
        inventory.saveMovement(movement(reservation.sku, CONFIRMATION, -reservation.quantity, "ORDER_RESERVATION", reservationKey, null, reservation.organization?.publicId))
        return purchaseCost
    }

    @Transactional
    fun restoreConfirmed(reservationKey: UUID): StockViewDto {
        val reservation = reservationEntity(reservationKey)
        if (reservation.status == RESTORED) return stock(reservation.sku.publicId ?: error("SKU 식별자가 없습니다."), reservation.organization?.publicId)
        require(reservation.status == CONFIRMED) { "확정된 재고만 복구할 수 있습니다." }
        val stock = inventory.lockedStock(reservation.sku, inventory.organizationId(reservation.organization?.publicId))
        stock.onHandQuantity += reservation.quantity
        val allocations = inventory.allocations(requireNotNull(reservation.id)).filter { it.status == ALLOCATION_CONFIRMED }
        allocations.forEach { allocation ->
            allocation.lot.remainingQuantity += allocation.quantity
            allocation.lot.remainingCostAmount = Math.addExact(requireNotNull(allocation.lot.remainingCostAmount), requireNotNull(allocation.costAmount))
            allocation.status = ALLOCATION_RESTORED
            inventory.saveLot(allocation.lot)
            inventory.saveAllocation(allocation)
        }
        if (allocations.isEmpty()) {
            inventory.saveLot(InventoryCostLotEntity().apply {
                sku = reservation.sku
                organization = reservation.organization
                containerReference = "UNPRICED-RESTORE-${reservation.reservationKey}"
                receivedQuantity = reservation.quantity
                remainingQuantity = reservation.quantity
            })
        }
        val updated = inventory.saveStock(stock)
        reservation.status = RESTORED
        reservation.releasedAt = Instant.now()
        inventory.saveReservation(reservation)
        inventory.saveMovement(movement(reservation.sku, RESTOCK, reservation.quantity, "ORDER_CANCELLATION", reservationKey, null, reservation.organization?.publicId))
        return updated.toState().toView(unitsPerSale(reservation.sku.publicId ?: error("SKU 식별자가 없습니다."), reservation.organization?.publicId))
    }

    private fun sku(id: UUID) = entitySku(id).toReference()
    private fun consumeUnreservedLots(sku: ProductSkuEntity, organizationId: Long?, quantity: Int) {
        var remaining = quantity
        inventory.lockedLots(requireNotNull(sku.id), organizationId).forEach { lot ->
            val available = lot.remainingQuantity - lot.reservedQuantity
            val consumed = minOf(remaining, available)
            if (consumed > 0) {
                val oldQuantity = lot.remainingQuantity
                lot.remainingQuantity -= consumed
                lot.remainingCostAmount?.let { current -> lot.remainingCostAmount = current - Math.multiplyExact(current, consumed.toLong()) / oldQuantity }
                inventory.saveLot(lot)
                remaining -= consumed
            }
        }
        check(remaining == 0) { "재고 원가 lot 수량이 재고 원장과 일치하지 않습니다." }
    }
    private fun entitySku(id: UUID) = catalog.sku(id) ?: throw ItemNotFoundException("SKU를 찾을 수 없습니다.")
    private fun reservationEntity(key: UUID) = inventory.reservation(key) ?: throw ItemNotFoundException("재고 예약을 찾을 수 없습니다.")
    private fun StockStateDto.toView(unitsPerSale: Int = 1) = StockViewDto(sku.id, sku.code, onHand, reserved, available, safety, unitsPerSale)
    private fun unitsPerSale(skuId: UUID, organizationId: UUID?) =
        (organizationId?.let { catalog.salesOffer("WHOLESALE", skuId, it) } ?: catalog.salesOffer("WHOLESALE", skuId))?.unitsPerSale ?: 1
    private fun ProductSkuEntity.toReference() = SkuReferenceDto(requireNotNull(publicId), skuCode)
    private fun InventoryStockEntity.toState() = StockStateDto(sku.toReference(), onHandQuantity, reservedQuantity, safetyStockQuantity)
    private fun StockReservationEntity.toState() = ReservationStateDto(reservationKey, sku.toReference(), quantity, status, expiresAt, releasedAt)
    private fun com.buyeong.umji.api.persistence.jpa.inventory.entity.InventoryMovementEntity.toState() = MovementStateDto(
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
        const val ALLOCATION_RESERVED = "RESERVED"
        const val ALLOCATION_RELEASED = "RELEASED"
        const val ALLOCATION_CONFIRMED = "CONFIRMED"
        const val ALLOCATION_RESTORED = "RESTORED"
    }
}
