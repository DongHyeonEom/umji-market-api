package com.buyeong.umji.api.persistence.jpa.catalog

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProductOptionValueRepository : JpaRepository<ProductOptionValueEntity, Long> {
    fun findAllByPublicIdIn(publicIds: Collection<UUID>): List<ProductOptionValueEntity>

    fun findAllByOptionIdOrderByDisplayOrderAscIdAsc(optionId: Long): List<ProductOptionValueEntity>
}