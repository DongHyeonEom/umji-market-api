package com.buyeong.umji.api.persistence.jpa.catalog

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProductRepository : JpaRepository<ProductEntity, Long> {
    @EntityGraph(attributePaths = ["brand"])
    fun findAllByDisplayStatusAndSalesStatusAndDeletedAtIsNull(
        displayStatus: String,
        salesStatus: String,
        pageable: Pageable,
    ): Page<ProductEntity>

    @EntityGraph(attributePaths = ["category", "brand"])
    fun findByPublicIdAndDisplayStatusAndSalesStatusAndDeletedAtIsNull(
        publicId: UUID,
        displayStatus: String,
        salesStatus: String,
    ): ProductEntity?

    fun findByPublicIdAndDeletedAtIsNull(publicId: UUID): ProductEntity?

    @EntityGraph(attributePaths = ["category", "brand"])
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<ProductEntity>
}