package com.buyeong.umji.api.persistence.jpa.catalog.repository

import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface ProductSkuRepository : JpaRepository<ProductSkuEntity, Long> {
    fun findAllByProductIdAndSalesStatusOrderBySalePriceAsc(productId: Long, salesStatus: String): List<ProductSkuEntity>

    fun existsBySkuCode(skuCode: String): Boolean

    fun findByPublicId(publicId: UUID): ProductSkuEntity?

    fun findAllByProductIdOrderBySalePriceAsc(productId: Long): List<ProductSkuEntity>

    @EntityGraph(attributePaths = ["product", "product.brand"])
    fun findAllByProduct_DeletedAtIsNullAndProduct_DisplayStatusAndProduct_SalesStatusAndSalesStatus(
        displayStatus: String,
        salesStatus: String,
        skuSalesStatus: String,
        pageable: Pageable,
    ): Page<ProductSkuEntity>
}
