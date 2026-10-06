package com.buyeong.umji.api.order.application.port.out

import com.buyeong.umji.api.order.application.model.TaxInvoiceBuyer
import com.buyeong.umji.api.order.application.model.TaxInvoiceSupplier
import java.util.UUID

interface TaxInvoiceSupplierPort {
    fun supplier(): TaxInvoiceSupplier?
}

interface TaxInvoiceBuyerProfilePort {
    fun forAccount(accountPublicId: UUID): TaxInvoiceBuyer?
}
