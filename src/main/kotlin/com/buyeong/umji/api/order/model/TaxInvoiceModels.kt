package com.buyeong.umji.api.order.model

import java.time.LocalDate
import java.util.UUID

data class TaxInvoiceSupplier(
    val businessRegistrationNumber: String,
    val businessName: String,
    val representativeName: String,
    val businessAddress: String,
    val businessIndustry: String,
    val businessItem: String,
    val email: String,
)

data class TaxInvoiceBuyer(
    val organizationId: UUID,
    val businessRegistrationNumber: String?,
    val businessName: String?,
    val representativeName: String?,
    val postalCode: String?,
    val address1: String?,
    val address2: String?,
    val businessIndustry: String?,
    val businessItem: String?,
    val email: String?,
    val complete: Boolean,
)

data class TaxInvoiceSnapshot(
    val status: String,
    val supplier: TaxInvoiceSupplier,
    val buyer: TaxInvoiceBuyer,
    val writtenDate: LocalDate?,
    val supplyDate: LocalDate?,
    val supplyAmount: Long,
)

data class TaxInvoiceSnapshotDraft(
    val supplier: TaxInvoiceSupplier,
    val buyer: TaxInvoiceBuyer,
)
