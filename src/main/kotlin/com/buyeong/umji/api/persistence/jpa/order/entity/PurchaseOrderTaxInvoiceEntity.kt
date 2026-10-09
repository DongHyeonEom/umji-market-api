package com.buyeong.umji.api.persistence.jpa.order.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDate
import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import jakarta.persistence.ManyToOne

@Entity
@Table(name = "purchase_order_tax_invoice")
class PurchaseOrderTaxInvoiceEntity {
    @Id
    @Column(name = "order_id")
    var id: Long? = null

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    lateinit var order: PurchaseOrderEntity

    @Column(nullable = false, length = 30)
    lateinit var status: String

    @Column(name = "written_date")
    var writtenDate: LocalDate? = null

    @Column(name = "supply_date")
    var supplyDate: LocalDate? = null

    @Column(name = "supplier_registration_number", length = 30)
    var supplierRegistrationNumber: String? = null

    @Column(name = "supplier_business_name", length = 200)
    var supplierBusinessName: String? = null

    @Column(name = "supplier_name", length = 100)
    var supplierName: String? = null

    @Column(name = "supplier_address", length = 500)
    var supplierAddress: String? = null

    @Column(name = "supplier_industry", length = 100)
    var supplierIndustry: String? = null

    @Column(name = "supplier_item", length = 100)
    var supplierItem: String? = null

    @Column(name = "supplier_email", length = 255)
    var supplierEmail: String? = null

    @Column(name = "buyer_registration_number", length = 30)
    var buyerRegistrationNumber: String? = null

    @Column(name = "buyer_business_name", length = 200)
    var buyerBusinessName: String? = null

    @Column(name = "buyer_name", length = 100)
    var buyerName: String? = null

    @Column(name = "buyer_postal_code", length = 20)
    var buyerPostalCode: String? = null

    @Column(name = "buyer_address1", length = 255)
    var buyerAddress1: String? = null

    @Column(name = "buyer_address2", length = 255)
    var buyerAddress2: String? = null

    @Column(name = "buyer_industry", length = 100)
    var buyerIndustry: String? = null

    @Column(name = "buyer_item", length = 100)
    var buyerItem: String? = null

    @Column(name = "buyer_email", length = 255)
    var buyerEmail: String? = null

    @Column(name = "invoice_approval_number", length = 100)
    var invoiceApprovalNumber: String? = null

    @Column(name = "issued_at")
    var issuedAt: LocalDate? = null

    @Column(name = "supply_amount")
    var supplyAmount: Long? = null

    @Column(name = "tax_amount")
    var taxAmount: Long? = null

    @Column(name = "total_amount")
    var totalAmount: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by_account_id")
    var issuedBy: AccountEntity? = null
}
