package com.buyeong.umji.api.persistence.jpa.order.entity

import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
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
import java.time.LocalDate

@Entity
@Table(name = "purchase_order")
class PurchaseOrderEntity : DomainPublicEntity() {
    @Column(name = "sales_channel_code", nullable = false, length = 30)
    var salesChannelCode: String = "WHOLESALE"

    @Column(name = "order_number", nullable = false)
    lateinit var orderNumber: String

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: AccountEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    lateinit var organization: OrganizationEntity

    @Column(nullable = false)
    lateinit var status: String

    @Column(name = "subtotal_amount", nullable = false)
    var subtotalAmount: Long = 0

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Long = 0

    @Column(name = "tax_invoice_requested", nullable = false)
    var taxInvoiceRequested: Boolean = false

    @Column(name = "tax_invoice_status", length = 30)
    var taxInvoiceStatus: String? = null

    @Column(name = "tax_invoice_written_date")
    var taxInvoiceWrittenDate: LocalDate? = null

    @Column(name = "tax_invoice_supply_date")
    var taxInvoiceSupplyDate: LocalDate? = null

    @Column(name = "tax_invoice_supplier_registration_number", length = 30)
    var taxInvoiceSupplierRegistrationNumber: String? = null

    @Column(name = "tax_invoice_supplier_business_name", length = 200)
    var taxInvoiceSupplierBusinessName: String? = null

    @Column(name = "tax_invoice_supplier_name", length = 100)
    var taxInvoiceSupplierName: String? = null

    @Column(name = "tax_invoice_supplier_address", length = 500)
    var taxInvoiceSupplierAddress: String? = null

    @Column(name = "tax_invoice_supplier_industry", length = 100)
    var taxInvoiceSupplierIndustry: String? = null

    @Column(name = "tax_invoice_supplier_item", length = 100)
    var taxInvoiceSupplierItem: String? = null

    @Column(name = "tax_invoice_supplier_email", length = 255)
    var taxInvoiceSupplierEmail: String? = null

    @Column(name = "tax_invoice_buyer_registration_number", length = 30)
    var taxInvoiceBuyerRegistrationNumber: String? = null

    @Column(name = "tax_invoice_buyer_business_name", length = 200)
    var taxInvoiceBuyerBusinessName: String? = null

    @Column(name = "tax_invoice_buyer_name", length = 100)
    var taxInvoiceBuyerName: String? = null

    @Column(name = "tax_invoice_buyer_postal_code", length = 20)
    var taxInvoiceBuyerPostalCode: String? = null

    @Column(name = "tax_invoice_buyer_address1", length = 255)
    var taxInvoiceBuyerAddress1: String? = null

    @Column(name = "tax_invoice_buyer_address2", length = 255)
    var taxInvoiceBuyerAddress2: String? = null

    @Column(name = "tax_invoice_buyer_industry", length = 100)
    var taxInvoiceBuyerIndustry: String? = null

    @Column(name = "tax_invoice_buyer_item", length = 100)
    var taxInvoiceBuyerItem: String? = null

    @Column(name = "tax_invoice_buyer_email", length = 255)
    var taxInvoiceBuyerEmail: String? = null

    @Column(name = "deposit_bank_name")
    var depositBankName: String? = null

    @Column(name = "deposit_account_number")
    var depositAccountNumber: String? = null

    @Column(name = "deposit_account_holder")
    var depositAccountHolder: String? = null

    @Column(name = "shipping_recipient_name", length = 100)
    var shippingRecipientName: String? = null

    @Column(name = "shipping_recipient_phone", length = 30)
    var shippingRecipientPhone: String? = null

    @Column(name = "shipping_postal_code", length = 20)
    var shippingPostalCode: String? = null

    @Column(name = "shipping_address1", length = 255)
    var shippingAddress1: String? = null

    @Column(name = "shipping_address2", length = 255)
    var shippingAddress2: String? = null

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
