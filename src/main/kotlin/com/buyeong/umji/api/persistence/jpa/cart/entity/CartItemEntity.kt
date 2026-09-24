package com.buyeong.umji.api.persistence.jpa.cart

import com.buyeong.umji.api.persistence.jpa.catalog.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "cart_item")
class CartItemEntity : DomainPublicEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    lateinit var cart: CartEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    lateinit var sku: ProductSkuEntity

    @Column(nullable = false)
    var quantity: Int = 0
}