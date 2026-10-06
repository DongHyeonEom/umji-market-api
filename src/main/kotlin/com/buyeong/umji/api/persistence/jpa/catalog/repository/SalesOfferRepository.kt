package com.buyeong.umji.api.persistence.jpa.catalog.repository

import com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesOfferEntity
import java.util.UUID
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface SalesOfferRepository : JpaRepository<SalesOfferEntity, Long> {
    @EntityGraph(attributePaths = ["productSku", "productSku.product", "salesChannel"])
    fun findByPublicId(publicId: UUID): SalesOfferEntity?

    @EntityGraph(attributePaths = ["productSku", "productSku.product", "salesChannel"])
    fun findBySalesChannel_CodeAndProductSku_PublicId(channelCode: String, skuPublicId: UUID): SalesOfferEntity?

    fun findAllBySalesChannel_CodeAndSalesStatusAndProductSku_Product_IdOrderBySalePriceAsc(
        channelCode: String,
        salesStatus: String,
        productId: Long,
    ): List<SalesOfferEntity>

    fun findBySalesChannel_IdAndProductSku_Id(channelId: Long, skuId: Long): SalesOfferEntity?
    fun findAllBySalesChannel_IdAndProductSku_Product_Id(channelId: Long, productId: Long): List<SalesOfferEntity>
}
