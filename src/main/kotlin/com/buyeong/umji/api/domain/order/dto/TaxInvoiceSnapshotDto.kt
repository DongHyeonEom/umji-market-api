package com.buyeong.umji.api.domain.order.dto

import java.time.LocalDate

data class TaxInvoiceSnapshotDto(
    val status: String,
    val supplier: TaxInvoiceSupplierDto,
    val buyer: TaxInvoiceBuyerDto,
    val writtenDate: LocalDate?,
    val supplyDate: LocalDate?,
    val supplyAmount: Long,
    val approvalNumber: String? = null,
    val issuedAt: LocalDate? = null,
    val taxAmount: Long? = null,
    val totalAmount: Long? = null,
)