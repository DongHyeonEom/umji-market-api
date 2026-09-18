package com.buyeong.umji.api.catalog.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<CategoryEntity, Long> {
    fun findAllByDisplayStatusAndDeletedAtIsNullOrderByDisplayOrderAscNameAsc(displayStatus: String): List<CategoryEntity>
}
