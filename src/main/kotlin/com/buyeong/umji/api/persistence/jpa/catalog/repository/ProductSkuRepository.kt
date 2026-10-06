package com.buyeong.umji.api.persistence.jpa.catalog.repository

import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface ProductSkuRepository : JpaRepository<ProductSkuEntity, Long> {
    fun findAllByProductIdAndSalesStatusOrderBySalePriceAsc(productId: Long, salesStatus: String): List<ProductSkuEntity>

    fun existsBySkuCode(skuCode: String): Boolean

    fun findByPublicId(publicId: UUID): ProductSkuEntity?

    fun findAllByProductIdOrderBySalePriceAsc(productId: Long): List<ProductSkuEntity>
}
