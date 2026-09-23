package com.buyeong.umji.api.order.adapter.`in`.web

import com.buyeong.umji.api.auth.application.port.`in`.CurrentAccountPort
import com.buyeong.umji.api.order.application.model.OrderItemView
import com.buyeong.umji.api.order.application.model.OrderPage
import com.buyeong.umji.api.order.application.model.OrderView
import com.buyeong.umji.api.order.application.port.`in`.OrderUseCase
import com.buyeong.umji.api.order.model.OrderPageResponse
import com.buyeong.umji.api.order.model.OrderResponse
import com.buyeong.umji.api.order.model.OrderItemResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
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
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(): OrderResponse = orders.create(currentAccounts.activeAccountPublicId()).toResponse()

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): OrderPageResponse = orders.list(currentAccounts.activeAccountPublicId(), page, size).toResponse()

    @GetMapping("/{orderId}")
    fun detail(@PathVariable orderId: UUID): OrderResponse = orders.detail(currentAccounts.activeAccountPublicId(), orderId).toResponse()

    private fun OrderPage.toResponse() = OrderPageResponse(items.map { it.toResponse() }, page, size, totalElements, totalPages)

    private fun OrderView.toResponse() = OrderResponse(
        id, orderNumber, status, subtotalAmount, totalAmount, orderedAt, items.map { it.toResponse() },
    )

    private fun OrderItemView.toResponse() = OrderItemResponse(
        id, skuId, productName, skuName, skuCode, unitPrice, quantity, lineAmount, status,
    )
}
