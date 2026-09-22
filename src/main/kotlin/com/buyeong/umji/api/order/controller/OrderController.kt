package com.buyeong.umji.api.order.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.order.model.OrderPageResponse
import com.buyeong.umji.api.order.model.OrderResponse
import com.buyeong.umji.api.order.service.OrderService
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
    private val currentAccounts: CurrentAccountService,
    private val orders: OrderService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(): OrderResponse = orders.create(currentAccounts.activeAccount())

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): OrderPageResponse = orders.list(currentAccounts.activeAccount(), page, size)

    @GetMapping("/{orderId}")
    fun detail(@PathVariable orderId: UUID): OrderResponse = orders.detail(currentAccounts.activeAccount(), orderId)
}
