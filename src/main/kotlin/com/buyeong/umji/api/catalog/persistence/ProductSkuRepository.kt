package com.buyeong.umji.api.catalog.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ProductSkuRepository : JpaRepository<ProductSkuEntity, Long> {
    fun findAllByProductIdAndSalesStatusOrderBySalePriceAsc(productId: Long, salesStatus: String): List<ProductSkuEntity>
}
