package com.buyeong.umji.api.persistence.jpa.catalog

import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainSoftDeletableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "product")
class ProductEntity : DomainSoftDeletableEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    lateinit var category: CategoryEntity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    var brand: BrandEntity? = null

    @Column(nullable = false)
    lateinit var name: String

    @Column
    var description: String? = null

    @Column(name = "display_status", nullable = false)
    lateinit var displayStatus: String

    @Column(name = "sales_status", nullable = false)
    lateinit var salesStatus: String

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0
}