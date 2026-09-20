package com.buyeong.umji.api.catalog.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ProductOptionRepository : JpaRepository<ProductOptionEntity, Long> {
    fun findAllByProductIdOrderByDisplayOrderAscIdAsc(productId: Long): List<ProductOptionEntity>
}
