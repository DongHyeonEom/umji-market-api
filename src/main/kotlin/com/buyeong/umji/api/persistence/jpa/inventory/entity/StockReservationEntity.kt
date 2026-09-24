package com.buyeong.umji.api.persistence.jpa.inventory

import com.buyeong.umji.api.persistence.jpa.catalog.ProductSkuEntity
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
import java.util.UUID

@Entity
@Table(name = "stock_reservation")
class StockReservationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "reservation_key", nullable = false, unique = true)
    lateinit var reservationKey: UUID

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    lateinit var sku: ProductSkuEntity

    @Column(nullable = false)
    var quantity: Int = 0

    @Column(nullable = false)
    lateinit var status: String

    @Column(name = "expires_at")
    var expiresAt: Instant? = null

    @Column(name = "released_at")
    var releasedAt: Instant? = null
}