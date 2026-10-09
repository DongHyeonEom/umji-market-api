package com.buyeong.umji.api.persistence.jpa.catalog.repository

import com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesOfferEntity
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SalesOfferRepository : JpaRepository<SalesOfferEntity, Long> {
    @EntityGraph(attributePaths = ["productSku", "productSku.product", "salesChannel"])
    fun findByPublicId(publicId: UUID): SalesOfferEntity?

    @EntityGraph(attributePaths = ["productSku", "productSku.product", "salesChannel"])
    fun findFirstBySalesChannel_CodeAndProductSku_PublicIdAndSalesStatusOrderBySalePriceAsc(channelCode: String, skuPublicId: UUID, salesStatus: String): SalesOfferEntity?
    fun findBySalesChannel_CodeAndProductSku_PublicIdAndOrganization_PublicId(channelCode: String, skuPublicId: UUID, organizationPublicId: UUID): SalesOfferEntity?

    fun findAllBySalesChannel_CodeAndSalesStatusAndProductSku_Product_IdOrderBySalePriceAsc(
        channelCode: String,
        salesStatus: String,
        productId: Long,
    ): List<SalesOfferEntity>

    fun findFirstBySalesChannel_IdAndProductSku_IdAndOrganizationIsNull(channelId: Long, skuId: Long): SalesOfferEntity?
    fun findBySalesChannel_IdAndProductSku_IdAndOrganization_Id(channelId: Long, skuId: Long, organizationId: Long): SalesOfferEntity?
    fun findAllBySalesChannel_IdAndProductSku_Product_Id(channelId: Long, productId: Long): List<SalesOfferEntity>
}