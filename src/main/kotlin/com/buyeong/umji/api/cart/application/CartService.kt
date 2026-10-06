package com.buyeong.umji.api.cart.application

import com.buyeong.umji.api.cart.application.model.AddCartItemCommand
import com.buyeong.umji.api.cart.application.model.CartItemState
import com.buyeong.umji.api.cart.application.model.CartItemView
import com.buyeong.umji.api.cart.application.model.CartState
import com.buyeong.umji.api.cart.application.model.CartView
import com.buyeong.umji.api.cart.application.model.UpdateCartItemCommand
import com.buyeong.umji.api.cart.application.port.out.CartStorePort
import com.buyeong.umji.api.cart.application.port.out.SellableSkuQueryPort
import com.buyeong.umji.api.exception.ItemNotFoundException
import java.util.UUID

class CartService(private val carts: CartStorePort, private val skus: SellableSkuQueryPort) {
    fun cart(accountId: UUID): CartView = carts.find(accountId)?.toView() ?: CartView(emptyList())

    fun add(accountId: UUID, command: AddCartItemCommand): CartView {
        require((command.skuId != null) xor (command.salesOfferId != null)) { "SKU ID 또는 판매 오퍼 ID 중 하나만 지정해야 합니다." }
        val sku = command.salesOfferId?.let(skus::findOffer)
            ?: command.skuId?.let { skus.find(it, command.channelCode) }
            ?: throw ItemNotFoundException("판매 오퍼를 찾을 수 없습니다.")
        require(sku.channelCode == command.channelCode.uppercase()) { "선택한 오퍼가 요청한 판매 채널과 일치하지 않습니다." }
        require(sku.salesStatus == ON_SALE) { "판매 중인 오퍼만 장바구니에 담을 수 있습니다." }
        val current = carts.find(accountId) ?: CartState(accountId, emptyList())
        require(current.items.isEmpty() || current.items.all { it.sku.channelCode == sku.channelCode }) {
            "장바구니에는 한 판매 채널의 상품만 담을 수 있습니다. 현재 상품을 주문하거나 비워 주세요."
        }
        val matching = current.items.firstOrNull { it.sku.id == sku.id }
        val updated = if (matching == null) {
            current.copy(items = current.items + CartItemState(null, sku, command.quantity))
        } else {
            current.copy(items = current.items.map { if (it === matching) it.copy(quantity = Math.addExact(it.quantity, command.quantity)) else it })
        }
        return carts.save(updated).toView()
    }

    fun update(accountId: UUID, itemId: UUID, command: UpdateCartItemCommand): CartView {
        val cart = carts.find(accountId) ?: throw ItemNotFoundException("장바구니를 찾을 수 없습니다.")
        requireItem(cart, itemId)
        return carts.save(cart.copy(items = cart.items.map { if (it.id == itemId) it.copy(quantity = command.quantity) else it })).toView()
    }

    fun remove(accountId: UUID, itemId: UUID) {
        val cart = carts.find(accountId) ?: throw ItemNotFoundException("장바구니를 찾을 수 없습니다.")
        requireItem(cart, itemId)
        carts.save(cart.copy(items = cart.items.filterNot { it.id == itemId }))
    }

    fun clearForCheckout(accountId: UUID) {
        val cart = carts.find(accountId) ?: return
        carts.save(cart.copy(items = emptyList()))
    }

    private fun requireItem(cart: CartState, itemId: UUID) =
        cart.items.firstOrNull { it.id == itemId } ?: throw ItemNotFoundException("장바구니 항목을 찾을 수 없습니다.")

    private fun CartState.toView() = CartView(
        items.map {
            CartItemView(
                requireNotNull(it.id),
                it.sku.id,
                it.sku.code,
                it.sku.productName,
                it.sku.name,
                it.quantity,
                it.sku.price,
                it.sku.salesStatus,
                it.sku.salesOfferId,
                it.sku.channelCode,
            )
        },
    )

    private companion object {
        const val ON_SALE = "ON_SALE"
    }
}
