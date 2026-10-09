package com.buyeong.umji.api.persistence.jpa.catalog.repository

import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductImageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ProductImageRepository : JpaRepository<ProductImageEntity, Long> {
    fun findAllByProductIdOrderByDisplayOrderAscIdAsc(productId: Long): List<ProductImageEntity>

    @org.springframework.data.jpa.repository.Query(
        "select count(image) > 0 from ProductImageEntity image where image.storageKey = :storageKey and image.product.displayStatus = 'VISIBLE' and image.product.deletedAt is null",
    )
    fun existsPublicImageByStorageKey(@org.springframework.data.repository.query.Param("storageKey") storageKey: String): Boolean
}