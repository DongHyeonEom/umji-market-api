package com.buyeong.umji.api.order.dto

data class TaxInvoiceSnapshotDraftDto(
    val supplier: TaxInvoiceSupplierDto,
    val buyer: TaxInvoiceBuyerDto,
)