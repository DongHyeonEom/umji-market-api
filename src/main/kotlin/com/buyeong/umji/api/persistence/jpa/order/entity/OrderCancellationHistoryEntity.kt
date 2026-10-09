package com.buyeong.umji.api.persistence.jpa.order.entity

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
@Table(name = "order_cancellation_history")
class OrderCancellationHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    lateinit var order: PurchaseOrderEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    lateinit var requester: AccountEntity

    @Column(name = "request_status", nullable = false)
    lateinit var status: String

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    var processor: AccountEntity? = null

    @Column(name = "requested_at", nullable = false)
    var requestedAt: Instant = Instant.now()

    @Column(name = "processed_at")
    var processedAt: Instant? = null
}