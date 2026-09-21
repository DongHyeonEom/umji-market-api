package com.buyeong.umji.api.cart.service

import com.buyeong.umji.api.account.persistence.AccountEntity
import com.buyeong.umji.api.catalog.persistence.ProductSkuEntity
import com.buyeong.umji.api.catalog.persistence.ProductSkuRepository
import com.buyeong.umji.api.cart.model.AddCartItemRequest
import com.buyeong.umji.api.cart.model.CartItemResponse
import com.buyeong.umji.api.cart.model.CartResponse
import com.buyeong.umji.api.cart.model.UpdateCartItemRequest
import com.buyeong.umji.api.cart.persistence.CartEntity
import com.buyeong.umji.api.cart.persistence.CartItemEntity
import com.buyeong.umji.api.cart.persistence.CartRepository
import com.buyeong.umji.api.exception.ItemNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CartService(
    private val carts: CartRepository,
    private val skus: ProductSkuRepository,
) {
    @Transactional(readOnly = true)
    fun cart(account: AccountEntity): CartResponse = carts.findWithItemsByAccountId(requireNotNull(account.id))?.let(::response) ?: CartResponse(emptyList())

    @Transactional
    fun add(account: AccountEntity, request: AddCartItemRequest): CartResponse {
        val sku = sellableSku(request.skuId)
        val cart = lockedCart(account)
        val existingItem = cart.items.firstOrNull { it.sku.id == sku.id }
        if (existingItem == null) {
            cart.add(CartItemEntity().apply { this.sku = sku; quantity = request.quantity })
        } else {
            existingItem.quantity = Math.addExact(existingItem.quantity, request.quantity)
        }
        return response(carts.saveAndFlush(cart))
    }

    @Transactional
    fun update(account: AccountEntity, itemId: UUID, request: UpdateCartItemRequest): CartResponse {
        val cart = lockedCart(account)
        item(cart, itemId).quantity = request.quantity
        return response(cart)
    }

    @Transactional
    fun remove(account: AccountEntity, itemId: UUID) {
        val cart = lockedCart(account)
        cart.remove(item(cart, itemId))
    }

    private fun lockedCart(account: AccountEntity): CartEntity =
        carts.findLockedByAccountId(requireNotNull(account.id)) ?: carts.save(CartEntity().apply { this.account = account })

    private fun sellableSku(skuId: UUID): ProductSkuEntity {
        val sku = skus.findByPublicId(skuId) ?: throw ItemNotFoundException("SKU를 찾을 수 없습니다.")
        require(sku.salesStatus == ON_SALE) { "판매 중인 SKU만 장바구니에 담을 수 있습니다." }
        return sku
    }

    private fun item(cart: CartEntity, itemId: UUID): CartItemEntity =
        cart.items.firstOrNull { it.publicId == itemId } ?: throw ItemNotFoundException("장바구니 항목을 찾을 수 없습니다.")

    private fun response(cart: CartEntity) = CartResponse(cart.items.map { item ->
        CartItemResponse(
            requireNotNull(item.publicId), requireNotNull(item.sku.publicId), item.sku.skuCode, item.sku.product.name,
            item.sku.name, item.quantity, item.sku.salePrice, item.sku.salesStatus,
        )
    })

    private companion object {
        const val ON_SALE = "ON_SALE"
    }
}
