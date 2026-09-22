package com.buyeong.umji.api.persistence.jpa.inventory

import com.buyeong.umji.api.persistence.jpa.catalog.ProductSkuEntity
import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainBaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "inventory_stock")
class InventoryStockEntity : DomainBaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sku_id", nullable = false)
    lateinit var sku: ProductSkuEntity

    @Column(name = "on_hand_quantity", nullable = false)
    var onHandQuantity: Int = 0

    @Column(name = "reserved_quantity", nullable = false)
    var reservedQuantity: Int = 0

    @Column(name = "safety_stock_quantity", nullable = false)
    var safetyStockQuantity: Int = 0

    fun availableQuantity(): Int = onHandQuantity - reservedQuantity
}
