package com.buyeong.umji.api.persistence.jpa.catalog.repository

import com.buyeong.umji.api.persistence.jpa.catalog.entity.BrandEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BrandRepository : JpaRepository<BrandEntity, Long> {
    fun existsByName(name: String): Boolean

    fun existsByOrganizationIsNullAndName(name: String): Boolean

    fun findByPublicIdAndDeletedAtIsNull(publicId: UUID): BrandEntity?

    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<BrandEntity>

    fun existsByOrganization_PublicIdAndNameAndDeletedAtIsNull(organizationId: UUID, name: String): Boolean

    fun findAllByOrganization_PublicIdAndDeletedAtIsNull(organizationId: UUID, pageable: Pageable): Page<BrandEntity>

    fun findByPublicIdAndOrganization_PublicIdAndDeletedAtIsNull(publicId: UUID, organizationId: UUID): BrandEntity?
}