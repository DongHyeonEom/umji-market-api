package com.buyeong.umji.api.cart.adapter.`in`.web

import com.buyeong.umji.api.cart.application.CartService
import com.buyeong.umji.api.cart.application.model.AddCartItemCommand
import com.buyeong.umji.api.cart.application.model.CartView
import com.buyeong.umji.api.cart.application.model.UpdateCartItemCommand
import com.buyeong.umji.api.cart.application.port.`in`.CartUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class TransactionalCartUseCase(private val carts: CartService) : CartUseCase {
    @Transactional(readOnly = true)
    override fun cart(accountId: UUID): CartView = carts.cart(accountId)

    @Transactional
    override fun add(accountId: UUID, command: AddCartItemCommand): CartView = carts.add(accountId, command)

    @Transactional
    override fun update(accountId: UUID, itemId: UUID, command: UpdateCartItemCommand): CartView = carts.update(accountId, itemId, command)

    @Transactional
    override fun remove(accountId: UUID, itemId: UUID) = carts.remove(accountId, itemId)

    @Transactional
    override fun clearForCheckout(accountId: UUID) = carts.clearForCheckout(accountId)
}
