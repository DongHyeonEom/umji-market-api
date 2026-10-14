package com.buyeong.umji.api.persistence.jpa.inventory.entity

import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.catalog.entity.ProductSkuEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "inventory_cost_lot")
class InventoryCostLotEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sku_id", nullable = false) lateinit var sku: ProductSkuEntity
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "organization_id") var organization: OrganizationEntity? = null
    @Column(name = "container_reference", nullable = false, length = 120) lateinit var containerReference: String
    @Column(name = "received_quantity", nullable = false) var receivedQuantity: Int = 0
    @Column(name = "remaining_quantity", nullable = false) var remainingQuantity: Int = 0
    @Column(name = "reserved_quantity", nullable = false) var reservedQuantity: Int = 0
    @Column(name = "total_purchase_cost") var totalPurchaseCost: Long? = null
    @Column(name = "remaining_cost_amount") var remainingCostAmount: Long? = null
    @Column(name = "created_at", nullable = false, updatable = false) var createdAt: Instant = Instant.now()
}
