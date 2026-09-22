package com.buyeong.umji.api.order.persistence

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
@Table(name = "order_status_history")
class OrderStatusHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    lateinit var order: PurchaseOrderEntity

    @Column(name = "from_status")
    var fromStatus: String? = null

    @Column(name = "to_status", nullable = false)
    lateinit var toStatus: String

    @Column(name = "changed_at", nullable = false)
    var changedAt: Instant = Instant.now()
}
