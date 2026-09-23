package com.buyeong.umji.api.order.adapter.out.persistence

import com.buyeong.umji.api.cart.application.port.`in`.CartUseCase
import com.buyeong.umji.api.order.application.model.CheckoutLine
import com.buyeong.umji.api.order.application.port.out.CheckoutCartPort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CartUseCaseCheckoutAdapter(private val carts: CartUseCase) : CheckoutCartPort {
    override fun linesForCheckout(accountId: UUID): List<CheckoutLine> = carts.cart(accountId).items.map {
        CheckoutLine(it.skuId, it.skuCode, it.productName, it.skuName, it.unitPrice, it.quantity, it.salesStatus)
    }

    override fun clear(accountId: UUID) = carts.clearForCheckout(accountId)
}
