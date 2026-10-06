package com.buyeong.umji.api.persistence.jpa.catalog.repository

import com.buyeong.umji.api.persistence.jpa.catalog.entity.BrandEntity
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface BrandRepository : JpaRepository<BrandEntity, Long> {
    fun existsByName(name: String): Boolean

    fun findByPublicIdAndDeletedAtIsNull(publicId: UUID): BrandEntity?

    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<BrandEntity>
}
