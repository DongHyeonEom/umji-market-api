package com.buyeong.umji.api.persistence.jpa.catalog.repository

import com.buyeong.umji.api.persistence.jpa.catalog.entity.CategoryEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<CategoryEntity, Long> {
    fun findAllBySalesChannel_CodeAndDisplayStatusAndDeletedAtIsNullOrderByDisplayOrderAscNameAsc(channelCode: String, displayStatus: String): List<CategoryEntity>

    fun findByPublicIdAndDeletedAtIsNull(publicId: UUID): CategoryEntity?

    fun findAllBySalesChannel_CodeAndDeletedAtIsNullOrderByDisplayOrderAscNameAsc(channelCode: String): List<CategoryEntity>
}
