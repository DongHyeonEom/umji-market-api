package com.buyeong.umji.api.persistence.jpa.catalog.entity

import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "channel_product_listing")
class ChannelProductListingEntity : DomainPublicEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_channel_id", nullable = false)
    lateinit var salesChannel: SalesChannelEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    lateinit var product: ProductEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    lateinit var category: CategoryEntity

    @Column(name = "display_status", nullable = false, length = 30)
    lateinit var displayStatus: String

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0
}