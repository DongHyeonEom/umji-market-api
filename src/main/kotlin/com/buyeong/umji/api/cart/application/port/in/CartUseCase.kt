package com.buyeong.umji.api.cart.application.port.`in`

import com.buyeong.umji.api.cart.application.model.AddCartItemCommand
import com.buyeong.umji.api.cart.application.model.CartView
import com.buyeong.umji.api.cart.application.model.UpdateCartItemCommand
import java.util.UUID

interface CartUseCase {
    fun cart(accountId: UUID): CartView
    fun add(accountId: UUID, command: AddCartItemCommand): CartView
    fun update(accountId: UUID, itemId: UUID, command: UpdateCartItemCommand): CartView
    fun remove(accountId: UUID, itemId: UUID)
    fun clearForCheckout(accountId: UUID)
}