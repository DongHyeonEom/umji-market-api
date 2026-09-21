package com.buyeong.umji.api.inventory.persistence

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface InventoryStockRepository : JpaRepository<InventoryStockEntity, Long> {
    fun findBySkuId(skuId: Long): InventoryStockEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stock from InventoryStockEntity stock where stock.sku.id = :skuId")
    fun findLockedBySkuId(@Param("skuId") skuId: Long): InventoryStockEntity?
}
