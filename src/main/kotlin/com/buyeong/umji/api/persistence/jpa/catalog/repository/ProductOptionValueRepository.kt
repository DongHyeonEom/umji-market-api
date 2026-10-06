package com.buyeong.umji.api.persistence.jpa.catalog.repository

import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductOptionValueEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface ProductOptionValueRepository : JpaRepository<ProductOptionValueEntity, Long> {
    fun findAllByPublicIdIn(publicIds: Collection<UUID>): List<ProductOptionValueEntity>

    fun findAllByOptionIdOrderByDisplayOrderAscIdAsc(optionId: Long): List<ProductOptionValueEntity>
}
