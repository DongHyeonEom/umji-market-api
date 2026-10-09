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
import java.time.LocalDate

@Entity
@Table(name = "purchase_order_tax_invoice_event")
class PurchaseOrderTaxInvoiceEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    lateinit var order: PurchaseOrderEntity

    @Column(name = "event_type", nullable = false, length = 30)
    lateinit var eventType: String

    @Column(name = "invoice_approval_number", nullable = false, length = 100)
    lateinit var invoiceApprovalNumber: String

    @Column(name = "issued_at", nullable = false)
    lateinit var issuedAt: LocalDate

    @Column(name = "written_date")
    var writtenDate: LocalDate? = null

    @Column(name = "supply_date")
    var supplyDate: LocalDate? = null

    @Column(name = "supply_amount", nullable = false)
    var supplyAmount: Long = 0

    @Column(name = "tax_amount", nullable = false)
    var taxAmount: Long = 0

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Long = 0

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processed_by_account_id", nullable = false)
    lateinit var processedBy: AccountEntity

    @Column(length = 500)
    var reason: String? = null

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
}