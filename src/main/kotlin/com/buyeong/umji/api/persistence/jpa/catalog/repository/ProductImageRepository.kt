package com.buyeong.umji.api.persistence.jpa.catalog.repository

import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductImageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ProductImageRepository : JpaRepository<ProductImageEntity, Long> {
    fun findAllByProductIdOrderByDisplayOrderAscIdAsc(productId: Long): List<ProductImageEntity>
}
