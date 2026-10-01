package com.buyeong.umji.api.persistence.jpa.order

import com.buyeong.umji.api.persistence.jpa.account.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupEntity
import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import com.buyeong.umji.api.persistence.jpa.order.OrderShipmentEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_group_id", nullable = false)
    lateinit var buyerGroup: BuyerGroupEntity

    @Column(nullable = false)
    lateinit var status: String

    @Column(name = "subtotal_amount", nullable = false)
    var subtotalAmount: Long = 0

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Long = 0

    @Column(name = "tax_invoice_requested", nullable = false)
    var taxInvoiceRequested: Boolean = false

    @Column(name = "deposit_bank_name")
    var depositBankName: String? = null

    @Column(name = "deposit_account_number")
    var depositAccountNumber: String? = null

    @Column(name = "deposit_account_holder")
    var depositAccountHolder: String? = null

    @Column(name = "ordered_at", nullable = false)
    var orderedAt: Instant = Instant.now()

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<OrderItemEntity> = mutableListOf()

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY, optional = false)
    lateinit var payment: OrderPaymentEntity

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY, optional = false)
    lateinit var shipment: OrderShipmentEntity

    fun add(item: OrderItemEntity) {
        item.order = this
        items.add(item)
    }
}