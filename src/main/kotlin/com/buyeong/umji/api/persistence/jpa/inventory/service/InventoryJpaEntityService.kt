package com.buyeong.umji.api.persistence.jpa.inventory

import com.buyeong.umji.api.persistence.jpa.catalog.ProductSkuEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class InventoryJpaEntityService(
    private val stocks: InventoryStockRepository,
    private val movements: InventoryMovementRepository,
    private val reservations: StockReservationRepository,
) {
    fun stock(skuId: Long): InventoryStockEntity? = stocks.findBySkuId(skuId)
    fun movements(skuId: Long, pageable: Pageable): Page<InventoryMovementEntity> = movements.findAllBySkuId(skuId, pageable)
    fun reservation(key: UUID): StockReservationEntity? = reservations.findByReservationKey(key)

    @Transactional
    fun lockedStock(sku: ProductSkuEntity): InventoryStockEntity =
        stocks.findLockedBySkuId(requireNotNull(sku.id)) ?: stocks.save(InventoryStockEntity().apply { this.sku = sku })

    @Transactional fun saveStock(stock: InventoryStockEntity): InventoryStockEntity = stocks.save(stock)

    @Transactional fun saveMovement(movement: InventoryMovementEntity): InventoryMovementEntity = movements.save(movement)

    @Transactional fun saveReservation(reservation: StockReservationEntity): StockReservationEntity = reservations.save(reservation)
}