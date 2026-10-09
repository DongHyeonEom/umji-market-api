package com.buyeong.umji.api.domain.operation.order.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class OperationManualTaxInvoiceRequest(
    @field:NotBlank
    @field:Size(max = 100,) val approvalNumber: String,
    @field:NotNull val issuedAt: LocalDate,
    @field:NotNull val writtenDate: LocalDate,
    @field:NotNull val supplyDate: LocalDate,
    @field:PositiveOrZero val supplyAmount: Long,
    @field:PositiveOrZero val taxAmount: Long,
    @field:PositiveOrZero val totalAmount: Long,
    @field:Size(max = 500,) val reason: String? = null,
)