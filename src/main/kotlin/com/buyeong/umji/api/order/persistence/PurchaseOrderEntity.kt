package com.buyeong.umji.api.order.persistence

import com.buyeong.umji.api.account.persistence.AccountEntity
import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "purchase_order")
class PurchaseOrderEntity : DomainPublicEntity() {
    @Column(name = "order_number", nullable = false)
    lateinit var orderNumber: String

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: AccountEntity

    @Column(nullable = false)
    lateinit var status: String

    @Column(name = "subtotal_amount", nullable = false)
    var subtotalAmount: Long = 0

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Long = 0

    @Column(name = "ordered_at", nullable = false)
    var orderedAt: Instant = Instant.now()

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<OrderItemEntity> = mutableListOf()

    fun add(item: OrderItemEntity) {
        item.order = this
        items.add(item)
    }
}
