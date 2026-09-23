package com.buyeong.umji.api.persistence.jpa.order

import com.buyeong.umji.api.persistence.jpa.catalog.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "order_item")
class OrderItemEntity : DomainPublicEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    lateinit var order: PurchaseOrderEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    lateinit var sku: ProductSkuEntity

    @Column(name = "product_name", nullable = false)
    lateinit var productName: String

    @Column(name = "sku_name", nullable = false)
    lateinit var skuName: String

    @Column(name = "sku_code", nullable = false)
    lateinit var skuCode: String

    @Column(name = "unit_price", nullable = false)
    var unitPrice: Long = 0

    @Column(nullable = false)
    var quantity: Int = 0

    @Column(name = "line_amount", nullable = false)
    var lineAmount: Long = 0

    @Column(name = "reservation_key", nullable = false, unique = true)
    lateinit var reservationKey: UUID

    @Column(nullable = false)
    lateinit var status: String
}
