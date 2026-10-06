package com.buyeong.umji.api.persistence.jpa.inventory.service

import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.inventory.entity.InventoryMovementEntity
import com.buyeong.umji.api.persistence.jpa.inventory.entity.InventoryStockEntity
import com.buyeong.umji.api.persistence.jpa.inventory.entity.StockReservationEntity
import com.buyeong.umji.api.persistence.jpa.inventory.repository.InventoryMovementRepository
import com.buyeong.umji.api.persistence.jpa.inventory.repository.InventoryStockRepository
import com.buyeong.umji.api.persistence.jpa.inventory.repository.StockReservationRepository
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
