package com.buyeong.umji.api.persistence.jpa.inventory.repository

import com.buyeong.umji.api.persistence.jpa.inventory.entity.InventoryCostLotEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface InventoryCostLotRepository : JpaRepository<InventoryCostLotEntity, Long> {
    @Query("select l from InventoryCostLotEntity l where l.sku.id = :skuId and ((:organizationId is null and l.organization is null) or l.organization.id = :organizationId) order by l.id")
    fun findBySkuAndOrganization(@Param("skuId") skuId: Long, @Param("organizationId") organizationId: Long?): List<InventoryCostLotEntity>
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from InventoryCostLotEntity l where l.sku.id = :skuId and ((:organizationId is null and l.organization is null) or l.organization.id = :organizationId) order by l.id")
    fun findLockedBySkuAndOrganization(@Param("skuId") skuId: Long, @Param("organizationId") organizationId: Long?): List<InventoryCostLotEntity>
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from InventoryCostLotEntity l where l.id = :id")
    fun findLockedById(@Param("id") id: Long): InventoryCostLotEntity?
}
