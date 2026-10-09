package com.buyeong.umji.api.operation.order.model

import com.buyeong.umji.api.order.model.AdminPhoneOrderLine
import com.buyeong.umji.api.order.model.ShippingAddressSnapshot
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.util.UUID

data class OperationPhoneOrderBuyerResponse(
    val accountId: UUID,
    val accountName: String,
    val phone: String,
    val organizationId: UUID,
    val organizationName: String,
)

data class OperationPhoneOrderRequest(
    val buyerAccountId: UUID,

    @field:NotBlank @field:Size(max = 100,) val recipientName: String,
    @field:NotBlank @field:Size(max = 30,) val recipientPhone: String,
    @field:NotBlank @field:Size(max = 20,) val postalCode: String,
    @field:NotBlank @field:Size(max = 255,) val address1: String,
    @field:Size(max = 255,) val address2: String? = null,
    @field:NotEmpty @field:Size(max = 100,) @field:Valid val items: List<OperationPhoneOrderLineRequest>,
    val taxInvoiceRequested: Boolean = false,
) {
    fun shippingAddress() = ShippingAddressSnapshot(recipientName.trim(), recipientPhone.trim(), postalCode.trim(), address1.trim(), address2?.trim())
    fun orderLines() = items.map { AdminPhoneOrderLine(it.salesOfferId, it.quantity) }
}

data class OperationPhoneOrderLineRequest(
    val salesOfferId: UUID,
    val quantity: Int,
)

data class OperationPhoneOrderResponse(
    val orders: List<OperationPhoneOrderSummary>,
)

data class OperationPhoneOrderSummary(
    val id: UUID,
    val orderNumber: String,
    val status: String,
    val totalAmount: Long,
    val orderedAt: java.time.Instant,
    val sellerOrganizationId: UUID?,
)
