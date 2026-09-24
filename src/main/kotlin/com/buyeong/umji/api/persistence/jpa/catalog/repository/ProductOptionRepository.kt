package com.buyeong.umji.api.persistence.jpa.catalog

import org.springframework.data.jpa.repository.JpaRepository

interface ProductOptionRepository : JpaRepository<ProductOptionEntity, Long> {
    fun findAllByProductIdOrderByDisplayOrderAscIdAsc(productId: Long): List<ProductOptionEntity>
}