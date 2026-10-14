package com.buyeong.umji.api.persistence.jpa.inventory.entity

import jakarta.persistence.*

@Entity
@Table(name = "stock_reservation_lot_allocation")
class StockReservationLotAllocationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reservation_id", nullable = false) lateinit var reservation: StockReservationEntity
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lot_id", nullable = false) lateinit var lot: InventoryCostLotEntity
    @Column(nullable = false) var quantity: Int = 0
    @Column(name = "cost_amount") var costAmount: Long? = null
    @Column(nullable = false, length = 20) var status: String = "RESERVED"
}
