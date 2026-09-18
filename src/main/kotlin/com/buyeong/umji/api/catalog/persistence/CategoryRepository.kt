package com.buyeong.umji.api.catalog.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CategoryRepository : JpaRepository<CategoryEntity, Long> {
    fun findAllByDisplayStatusAndDeletedAtIsNullOrderByDisplayOrderAscNameAsc(displayStatus: String): List<CategoryEntity>

    fun findByPublicIdAndDeletedAtIsNull(publicId: UUID): CategoryEntity?

    fun findAllByDeletedAtIsNullOrderByDisplayOrderAscNameAsc(): List<CategoryEntity>
}
