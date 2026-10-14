package com.buyeong.umji.api.persistence.jpa.order.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "order_shipment_package_item")
class OrderShipmentPackageItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    lateinit var shipmentPackage: OrderShipmentPackageEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    lateinit var orderItem: OrderItemEntity

    @Column(name = "box_quantity", nullable = false)
    var boxQuantity: Int = 0

    @Column(name = "amount_per_box_snapshot")
    var amountPerBoxSnapshot: Long? = null

    @Column(name = "line_shipping_cost_snapshot")
    var lineShippingCostSnapshot: Long? = null
}
