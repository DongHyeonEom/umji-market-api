package com.buyeong.umji.api.persistence.jpa.catalog.entity

import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "brand_shipping_rate")
class BrandShippingRateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    lateinit var brand: BrandEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    lateinit var sku: ProductSkuEntity

    @Column(name = "carrier_code", nullable = false)
    lateinit var carrierCode: String

    @Column(name = "amount_per_box", nullable = false)
    var amountPerBox: Long = 0

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "updated_by_account_id", nullable = false)
    lateinit var updatedBy: AccountEntity

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
}
