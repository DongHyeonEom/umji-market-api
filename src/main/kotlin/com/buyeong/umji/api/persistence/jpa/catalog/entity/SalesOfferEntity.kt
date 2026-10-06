package com.buyeong.umji.api.persistence.jpa.catalog.entity

import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "sales_offer")
class SalesOfferEntity : DomainPublicEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_channel_id", nullable = false)
    lateinit var salesChannel: SalesChannelEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_sku_id", nullable = false)
    lateinit var productSku: ProductSkuEntity

    @Column(name = "sale_price", nullable = false)
    var salePrice: Long = 0

    @Column(name = "list_price")
    var listPrice: Long? = null

    @Column(name = "units_per_sale", nullable = false)
    var unitsPerSale: Int = 1

    @Column(name = "sales_status", nullable = false, length = 30)
    lateinit var salesStatus: String
}
