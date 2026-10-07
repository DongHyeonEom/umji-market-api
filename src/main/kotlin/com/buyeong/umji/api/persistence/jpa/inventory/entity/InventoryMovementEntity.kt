package com.buyeong.umji.api.persistence.jpa.inventory.entity

import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
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
@Table(name = "inventory_movement")
class InventoryMovementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    lateinit var sku: ProductSkuEntity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    var organization: OrganizationEntity? = null

    @Column(name = "movement_type", nullable = false)
    lateinit var movementType: String

    @Column(name = "quantity_delta", nullable = false)
    var quantityDelta: Int = 0

    @Column(name = "reference_type")
    var referenceType: String? = null

    @Column(name = "reference_id")
    var referenceId: UUID? = null

    @Column
    var memo: String? = null

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: Instant = Instant.now()
}
