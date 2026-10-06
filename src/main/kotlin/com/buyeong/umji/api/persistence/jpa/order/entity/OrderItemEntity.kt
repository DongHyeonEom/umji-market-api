package com.buyeong.umji.api.persistence.jpa.order.entity

import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesOfferEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "order_item")
class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "public_id", nullable = false, updatable = false)
    var publicId: UUID? = null
        protected set

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    lateinit var order: PurchaseOrderEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    lateinit var sku: ProductSkuEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_offer_id", nullable = false)
    lateinit var salesOffer: SalesOfferEntity

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

    @Column(name = "units_per_sale", nullable = false)
    var unitsPerSale: Int = 1

    @Column(name = "line_amount", nullable = false)
    var lineAmount: Long = 0

    @Column(name = "reservation_key", nullable = false, unique = true)
    lateinit var reservationKey: UUID

    @Column(nullable = false)
    lateinit var status: String

    @PrePersist
    protected fun assignPublicIdAndCreatedAt() {
        if (publicId == null) publicId = UUID.randomUUID()
        if (createdAt == null) createdAt = Instant.now()
    }
}
