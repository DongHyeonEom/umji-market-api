package com.buyeong.umji.api.catalog.persistence

import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "product_option_value")
class ProductOptionValueEntity : DomainPublicEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_option_id", nullable = false)
    lateinit var option: ProductOptionEntity

    @Column(nullable = false)
    lateinit var value: String

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0
}
