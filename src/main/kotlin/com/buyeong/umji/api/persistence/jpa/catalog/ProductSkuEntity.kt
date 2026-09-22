package com.buyeong.umji.api.persistence.jpa.catalog

import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.ManyToMany
import jakarta.persistence.JoinTable
import jakarta.persistence.Table

@Entity
@Table(name = "product_sku")
class ProductSkuEntity : DomainPublicEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    lateinit var product: ProductEntity

    @Column(name = "sku_code", nullable = false)
    lateinit var skuCode: String

    @Column(nullable = false)
    lateinit var name: String

    @Column(name = "sale_price", nullable = false)
    var salePrice: Long = 0

    @Column(name = "list_price")
    var listPrice: Long? = null

    @Column(name = "sales_status", nullable = false)
    lateinit var salesStatus: String

    @ManyToMany
    @JoinTable(
        name = "product_sku_option_value",
        joinColumns = [JoinColumn(name = "product_sku_id")],
        inverseJoinColumns = [JoinColumn(name = "product_option_value_id")],
    )
    var optionValues: MutableSet<ProductOptionValueEntity> = linkedSetOf()
}
