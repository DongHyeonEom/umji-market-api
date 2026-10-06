package com.buyeong.umji.api.persistence.jpa.catalog.repository

import com.buyeong.umji.api.persistence.jpa.catalog.entity.ChannelProductListingEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ChannelProductListingRepository : JpaRepository<ChannelProductListingEntity, Long> {
    @Query("select c from CategoryEntity c where c.salesChannel.code = :channel and c.displayStatus = :status and c.deletedAt is null order by c.displayOrder, c.name")
    fun findCategories(@Param("channel") channel: String, @Param("status") status: String): List<com.buyeong.umji.api.persistence.jpa.catalog.CategoryEntity>

    @EntityGraph(attributePaths = ["product", "product.brand", "category"])
    @Query(
        "select l from ChannelProductListingEntity l where l.salesChannel.code = :channel and l.displayStatus = :displayStatus and l.product.deletedAt is null and exists (select o.id from SalesOfferEntity o where o.salesChannel.id = l.salesChannel.id and o.productSku.product.id = l.product.id and o.salesStatus = :salesStatus)",
    )
    fun findPublicListings(
        @Param("channel") channel: String,
        @Param("displayStatus") displayStatus: String,
        @Param("salesStatus") salesStatus: String,
        pageable: Pageable,
    ): Page<ChannelProductListingEntity>

    @EntityGraph(attributePaths = ["product", "product.brand", "category"])
    @Query(
        "select l from ChannelProductListingEntity l where l.salesChannel.code = :channel and l.product.publicId = :productId and l.displayStatus = :displayStatus and l.product.deletedAt is null and exists (select o.id from SalesOfferEntity o where o.salesChannel.id = l.salesChannel.id and o.productSku.product.id = l.product.id and o.salesStatus = :salesStatus)",
    )
    fun findPublicListing(
        @Param("channel") channel: String,
        @Param("productId") productId: UUID,
        @Param("displayStatus") displayStatus: String,
        @Param("salesStatus") salesStatus: String,
    ): ChannelProductListingEntity?

    fun findBySalesChannel_IdAndProduct_Id(channelId: Long, productId: Long): ChannelProductListingEntity?
    fun findAllBySalesChannel_IdAndProduct_Id(channelId: Long, productId: Long): List<ChannelProductListingEntity>
}
