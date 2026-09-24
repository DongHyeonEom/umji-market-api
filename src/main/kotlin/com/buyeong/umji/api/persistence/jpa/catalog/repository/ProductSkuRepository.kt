package com.buyeong.umji.api.persistence.jpa.catalog

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProductSkuRepository : JpaRepository<ProductSkuEntity, Long> {
    fun findAllByProductIdAndSalesStatusOrderBySalePriceAsc(productId: Long, salesStatus: String): List<ProductSkuEntity>

    fun existsBySkuCode(skuCode: String): Boolean

    fun findByPublicId(publicId: UUID): ProductSkuEntity?

    fun findAllByProductIdOrderBySalePriceAsc(productId: Long): List<ProductSkuEntity>
}