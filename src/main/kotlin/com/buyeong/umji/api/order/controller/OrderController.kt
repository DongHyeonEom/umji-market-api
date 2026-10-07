package com.buyeong.umji.api.order.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.order.model.BankAccountInstructionsResponse
import com.buyeong.umji.api.order.model.CreateOrderRequest
import com.buyeong.umji.api.order.model.OrderCheckoutOptions
import com.buyeong.umji.api.order.model.OrderCheckoutOptionsResponse
import com.buyeong.umji.api.order.model.OrderItemResponse
import com.buyeong.umji.api.order.model.OrderItemView
import com.buyeong.umji.api.order.model.OrderPage
import com.buyeong.umji.api.order.model.OrderPageResponse
import com.buyeong.umji.api.order.model.OrderResponse
import com.buyeong.umji.api.order.model.OrderCheckoutResponse
import com.buyeong.umji.api.order.model.OrderView
import com.buyeong.umji.api.order.model.TaxInvoiceItemResponse
import com.buyeong.umji.api.order.model.TaxInvoiceSnapshotResponse
import com.buyeong.umji.api.order.service.CustomerOrderListingService
import com.buyeong.umji.api.order.service.OrderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
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

@RestController
@RequestMapping("/api/orders")
@Validated
@Tag(name = "주문", description = "구매자 그룹 주문 생성·조회와 결제 정보 API")
class OrderController(
    private val currentAccounts: CurrentAccountService,
    private val orders: OrderService,
    private val customerOrders: CustomerOrderListingService,
) {
    @Operation(summary = "주문 결제 정보조회", description = "구매자 그룹 주문 생성·조회와 결제 정보 API. /checkout-options 경로에서 주문 결제 정보조회를 수행")
    @GetMapping("/checkout-options")
    fun checkoutOptions(): OrderCheckoutOptionsResponse = orders.checkoutOptions(currentAccounts.activeAccountPublicId()).toResponse()

    @Operation(summary = "주문 생성", description = "주문 생성 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateOrderRequest): OrderCheckoutResponse =
        OrderCheckoutResponse(orders.create(
            currentAccounts.activeAccountPublicId(),
            request.shippingAddressId,
            request.taxInvoiceRequested,
            request.updateDefaultTaxInvoicePreference,
        ).map { it.toResponse() })

    @Operation(summary = "주문 목록 조회", description = "주문 목록 조회 기능을 수행하고 요청 조건에 따른 결과를 반환")
    @GetMapping
    fun list(
        @Parameter(description = "조회할 페이지 번호(0부터 시작)") @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "페이지당 조회할 항목 수") @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): OrderPageResponse {
        val customerId = currentAccounts.activeAccountPublicId()
        return customerOrders.list(customerId, page, size).toResponse()
    }

    @Operation(summary = "주문조회", description = "구매자 그룹 주문 생성·조회와 결제 정보 API. /{orderId} 경로에서 주문조회를 수행")
    @GetMapping("/{orderId}")
    fun detail(
        @Parameter(description = "대상 주문 공개 식별자(UUID)") @PathVariable orderId: UUID,
    ): OrderResponse = orders.detail(currentAccounts.activeAccountPublicId(), orderId).toResponse()

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
        taxInvoiceSnapshot?.let { snapshot ->
            TaxInvoiceSnapshotResponse(
                status = snapshot.status,
                writtenDate = snapshot.writtenDate,
                supplyDate = snapshot.supplyDate,
                supplierBusinessRegistrationNumber = snapshot.supplier.businessRegistrationNumber,
                supplierBusinessName = snapshot.supplier.businessName,
                supplierName = snapshot.supplier.representativeName,
                supplierAddress = snapshot.supplier.businessAddress,
                supplierIndustry = snapshot.supplier.businessIndustry,
                supplierItem = snapshot.supplier.businessItem,
                supplierEmail = snapshot.supplier.email,
                buyerBusinessRegistrationNumber = requireNotNull(snapshot.buyer.businessRegistrationNumber),
                buyerBusinessName = requireNotNull(snapshot.buyer.businessName),
                buyerName = requireNotNull(snapshot.buyer.representativeName),
                buyerPostalCode = requireNotNull(snapshot.buyer.postalCode),
                buyerAddress1 = requireNotNull(snapshot.buyer.address1),
                buyerAddress2 = snapshot.buyer.address2,
                buyerIndustry = requireNotNull(snapshot.buyer.businessIndustry),
                buyerItem = requireNotNull(snapshot.buyer.businessItem),
                buyerEmail = snapshot.buyer.email,
                items = items.map { TaxInvoiceItemResponse(it.productName, it.skuCode, it.quantity, it.lineAmount) },
                supplyAmount = snapshot.supplyAmount,
            )
        },
        channelCode,
        sellerOrganizationId,
    )

    private fun OrderCheckoutOptions.toResponse() = OrderCheckoutOptionsResponse(
        defaultTaxInvoiceRequested,
        taxInvoiceAvailable,
        standardBankAccount.toResponse(),
        taxInvoiceBankAccount.toResponse(),
    )

    private fun com.buyeong.umji.api.order.model.BankAccountInstructions.toResponse() =
        BankAccountInstructionsResponse(bankName, accountNumber, accountHolder)

    private fun OrderItemView.toResponse() = OrderItemResponse(
        id, skuId, productName, skuName, skuCode, unitPrice, quantity, lineAmount, status, salesOfferId, unitsPerSale,
    )
}
