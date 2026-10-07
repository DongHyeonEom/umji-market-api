package com.buyeong.umji.api.persistence.jpa.inventory.repository

import com.buyeong.umji.api.persistence.jpa.inventory.entity.InventoryStockEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface InventoryStockRepository : JpaRepository<InventoryStockEntity, Long> {
    fun findBySkuId(skuId: Long): InventoryStockEntity?

    @Query("select stock from InventoryStockEntity stock left join stock.organization organization where stock.sku.id = :skuId and ((:organizationId is null and organization is null) or organization.id = :organizationId)")
    fun findBySkuIdAndOrganizationId(@Param("skuId") skuId: Long, @Param("organizationId") organizationId: Long?): InventoryStockEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stock from InventoryStockEntity stock where stock.sku.id = :skuId")
    fun findLockedBySkuId(@Param("skuId") skuId: Long): InventoryStockEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select stock from InventoryStockEntity stock left join stock.organization organization where stock.sku.id = :skuId and ((:organizationId is null and organization is null) or organization.id = :organizationId)")
    fun findLockedBySkuIdAndOrganizationId(@Param("skuId") skuId: Long, @Param("organizationId") organizationId: Long?): InventoryStockEntity?
}
