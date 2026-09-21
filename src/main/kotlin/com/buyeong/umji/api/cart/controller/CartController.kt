package com.buyeong.umji.api.cart.controller

import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.cart.model.AddCartItemRequest
import com.buyeong.umji.api.cart.model.CartResponse
import com.buyeong.umji.api.cart.model.UpdateCartItemRequest
import com.buyeong.umji.api.cart.service.CartService
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
    private val currentAccounts: CurrentAccountService,
    private val carts: CartService,
) {
    @GetMapping
    fun cart(): CartResponse = carts.cart(currentAccounts.activeAccount())

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    fun add(@Valid @RequestBody request: AddCartItemRequest): CartResponse = carts.add(currentAccounts.activeAccount(), request)

    @PatchMapping("/items/{itemId}")
    fun update(@PathVariable itemId: UUID, @Valid @RequestBody request: UpdateCartItemRequest): CartResponse =
        carts.update(currentAccounts.activeAccount(), itemId, request)

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(@PathVariable itemId: UUID) {
        carts.remove(currentAccounts.activeAccount(), itemId)
    }
}
