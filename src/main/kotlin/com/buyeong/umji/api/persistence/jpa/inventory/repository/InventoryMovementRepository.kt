package com.buyeong.umji.api.persistence.jpa.inventory.repository

import com.buyeong.umji.api.persistence.jpa.inventory.entity.InventoryMovementEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface InventoryMovementRepository : JpaRepository<InventoryMovementEntity, Long> {
    fun findAllBySkuId(skuId: Long, pageable: Pageable): Page<InventoryMovementEntity>
}
