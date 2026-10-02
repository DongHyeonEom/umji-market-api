package com.buyeong.umji.api.order.adapter.`in`.web

import com.buyeong.umji.api.auth.application.port.`in`.CurrentAccountPort
import com.buyeong.umji.api.order.application.model.OrderCheckoutOptions
import com.buyeong.umji.api.order.application.model.OrderItemView
import com.buyeong.umji.api.order.application.model.OrderPage
import com.buyeong.umji.api.order.application.model.OrderView
import com.buyeong.umji.api.order.application.port.`in`.CustomerOrderListingUseCase
import com.buyeong.umji.api.order.application.port.`in`.OrderUseCase
import com.buyeong.umji.api.order.model.BankAccountInstructionsResponse
import com.buyeong.umji.api.order.model.CreateOrderRequest
import com.buyeong.umji.api.order.model.OrderCheckoutOptionsResponse
import com.buyeong.umji.api.order.model.OrderItemResponse
import com.buyeong.umji.api.order.model.OrderPageResponse
import com.buyeong.umji.api.order.model.OrderResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/orders")
@Validated
class OrderController(
    private val currentAccounts: CurrentAccountPort,
    private val orders: OrderUseCase,
    private val customerOrders: CustomerOrderListingUseCase,
) {
    @GetMapping("/checkout-options")
    fun checkoutOptions(): OrderCheckoutOptionsResponse = orders.checkoutOptions(currentAccounts.activeAccountPublicId()).toResponse()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateOrderRequest): OrderResponse =
        orders.create(
            currentAccounts.activeAccountPublicId(),
            request.shippingAddressId,
            request.taxInvoiceRequested,
            request.updateDefaultTaxInvoicePreference,
        ).toResponse()

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): OrderPageResponse {
        val customerId = currentAccounts.activeAccountPublicId()
        return customerOrders.list(customerId, page, size).toResponse()
    }

    @GetMapping("/{orderId}")
    fun detail(@PathVariable orderId: UUID): OrderResponse = orders.detail(currentAccounts.activeAccountPublicId(), orderId).toResponse()

    private fun OrderPage.toResponse() = OrderPageResponse(items.map { it.toResponse() }, page, size, totalElements, totalPages)

    private fun OrderView.toResponse() = OrderResponse(
        id,
        orderNumber,
        status,
        subtotalAmount,
        totalAmount,
        orderedAt,
        items.map { it.toResponse() },
        paymentMethod,
        paymentStatus,
        taxInvoiceRequested,
        depositBankName,
        depositAccountNumber,
        depositAccountHolder,
        shippingStatus,
        carrierCode,
        trackingNumber,
        CarrierTrackingLink.create(carrierCode, trackingNumber),
        cancellationRequestStatus,
        orderedByName,
        orderedByPhoneSuffix,
        shippingRecipientName,
        shippingRecipientPhone,
        shippingPostalCode,
        shippingAddress1,
        shippingAddress2,
    )

    private fun OrderCheckoutOptions.toResponse() = OrderCheckoutOptionsResponse(
        defaultTaxInvoiceRequested,
        standardBankAccount.toResponse(),
        taxInvoiceBankAccount.toResponse(),
    )

    private fun com.buyeong.umji.api.order.application.model.BankAccountInstructions.toResponse() =
        BankAccountInstructionsResponse(bankName, accountNumber, accountHolder)

    private fun OrderItemView.toResponse() = OrderItemResponse(
        id, skuId, productName, skuName, skuCode, unitPrice, quantity, lineAmount, status,
    )
}