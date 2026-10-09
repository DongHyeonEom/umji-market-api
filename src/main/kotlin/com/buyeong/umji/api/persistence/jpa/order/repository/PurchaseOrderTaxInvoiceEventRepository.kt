package com.buyeong.umji.api.persistence.jpa.order.repository

import com.buyeong.umji.api.persistence.jpa.order.entity.PurchaseOrderTaxInvoiceEventEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PurchaseOrderTaxInvoiceEventRepository : JpaRepository<PurchaseOrderTaxInvoiceEventEntity, Long> {
    fun existsByInvoiceApprovalNumber(invoiceApprovalNumber: String): Boolean
}