package com.buyeong.umji.api.operation.order.model

import com.buyeong.umji.api.order.dto.AdminPhoneOrderLineDto
import com.buyeong.umji.api.order.dto.ShippingAddressSnapshotDto
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.util.UUID

data class OperationPhoneOrderRequest(
    val buyerAccountId: UUID,

    @field:NotBlank
    @field:Size(max = 100,) val recipientName: String,
    @field:NotBlank
    @field:Size(max = 30,) val recipientPhone: String,
    @field:NotBlank
    @field:Size(max = 20,) val postalCode: String,
    @field:NotBlank
    @field:Size(max = 255,) val address1: String,
    @field:Size(max = 255,) val address2: String? = null,
    @field:NotEmpty @field:Size(max = 100,)
    @field:Valid val items: List<OperationPhoneOrderLineRequest>,
    val taxInvoiceRequested: Boolean = false,
) {
    fun shippingAddress() = ShippingAddressSnapshotDto(recipientName.trim(), recipientPhone.trim(), postalCode.trim(), address1.trim(), address2?.trim())
    fun orderLines() = items.map { AdminPhoneOrderLineDto(it.salesOfferId, it.quantity) }
}