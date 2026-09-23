package com.buyeong.umji.api.cart.adapter.`in`.web

import com.buyeong.umji.api.auth.application.port.`in`.CurrentAccountPort
import com.buyeong.umji.api.cart.model.AddCartItemRequest
import com.buyeong.umji.api.cart.model.CartResponse
import com.buyeong.umji.api.cart.model.UpdateCartItemRequest
import com.buyeong.umji.api.cart.application.model.AddCartItemCommand
import com.buyeong.umji.api.cart.application.model.CartView
import com.buyeong.umji.api.cart.application.model.UpdateCartItemCommand
import com.buyeong.umji.api.cart.application.port.`in`.CartUseCase
import com.buyeong.umji.api.cart.model.CartItemResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/cart")
class CartController(
    private val currentAccounts: CurrentAccountPort,
    private val carts: CartUseCase,
) {
    @GetMapping
    fun cart(): CartResponse = carts.cart(currentAccounts.activeAccountPublicId()).toResponse()

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    fun add(@Valid @RequestBody request: AddCartItemRequest): CartResponse = carts.add(
        currentAccounts.activeAccountPublicId(), AddCartItemCommand(request.skuId, request.quantity),
    ).toResponse()

    @PatchMapping("/items/{itemId}")
    fun update(@PathVariable itemId: UUID, @Valid @RequestBody request: UpdateCartItemRequest): CartResponse =
        carts.update(currentAccounts.activeAccountPublicId(), itemId, UpdateCartItemCommand(request.quantity)).toResponse()

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(@PathVariable itemId: UUID) {
        carts.remove(currentAccounts.activeAccountPublicId(), itemId)
    }

    private fun CartView.toResponse() = CartResponse(items.map {
        CartItemResponse(it.id, it.skuId, it.skuCode, it.productName, it.skuName, it.quantity, it.unitPrice, it.salesStatus)
    })
}
