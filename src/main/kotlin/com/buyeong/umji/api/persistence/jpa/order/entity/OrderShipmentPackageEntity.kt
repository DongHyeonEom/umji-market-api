package com.buyeong.umji.api.persistence.jpa.order.entity

import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.BrandEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "order_shipment_package")
class OrderShipmentPackageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "public_id", nullable = false, updatable = false)
    var publicId: UUID? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_shipment_id", nullable = false)
    lateinit var shipment: OrderShipmentEntity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    var brand: BrandEntity? = null

    @Column(name = "carrier_code", nullable = false)
    lateinit var carrierCode: String

    @Column(name = "tracking_number", nullable = false)
    lateinit var trackingNumber: String

    @Column(name = "actual_shipping_cost")
    var actualShippingCost: Long? = null

    @Column(nullable = false)
    var status: String = "IN_TRANSIT"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    var processedBy: AccountEntity? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @OneToMany(mappedBy = "shipmentPackage", cascade = [jakarta.persistence.CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<OrderShipmentPackageItemEntity> = mutableListOf()

    @PrePersist
    fun assignPublicId() {
        if (publicId == null) publicId = UUID.randomUUID()
    }
}
