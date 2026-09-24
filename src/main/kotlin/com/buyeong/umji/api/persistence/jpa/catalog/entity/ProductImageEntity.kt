package com.buyeong.umji.api.persistence.jpa.catalog

import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "product_image")
class ProductImageEntity : DomainPublicEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    lateinit var product: ProductEntity

    @Column(name = "storage_key", nullable = false)
    lateinit var storageKey: String

    @Column(name = "alt_text")
    var altText: String? = null

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0
}