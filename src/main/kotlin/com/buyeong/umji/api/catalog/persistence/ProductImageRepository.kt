package com.buyeong.umji.api.catalog.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ProductImageRepository : JpaRepository<ProductImageEntity, Long> {
    fun findAllByProductIdOrderByDisplayOrderAscIdAsc(productId: Long): List<ProductImageEntity>
}
