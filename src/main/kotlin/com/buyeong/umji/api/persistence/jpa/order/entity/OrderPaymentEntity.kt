package com.buyeong.umji.api.persistence.jpa.order

import com.buyeong.umji.api.persistence.jpa.account.AccountEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "order_payment")
class OrderPaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    lateinit var order: PurchaseOrderEntity

    @Column(name = "payment_method", nullable = false)
    lateinit var paymentMethod: String

    @Column(nullable = false)
    lateinit var status: String

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @OneToMany(mappedBy = "payment")
    var history: MutableList<OrderPaymentHistoryEntity> = mutableListOf()
}

@Entity
@Table(name = "order_payment_status_history")
class OrderPaymentHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    lateinit var payment: OrderPaymentEntity

    @Column(name = "from_status")
    var fromStatus: String? = null

    @Column(name = "to_status", nullable = false)
    lateinit var toStatus: String

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    var processedBy: AccountEntity? = null

    @Column(name = "changed_at", nullable = false)
    var changedAt: Instant = Instant.now()
}
