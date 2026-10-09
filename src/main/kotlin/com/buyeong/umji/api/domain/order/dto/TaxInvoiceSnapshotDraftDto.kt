package com.buyeong.umji.api.domain.order.dto

data class TaxInvoiceSnapshotDraftDto(
    val supplier: TaxInvoiceSupplierDto,
    val buyer: TaxInvoiceBuyerDto,
)