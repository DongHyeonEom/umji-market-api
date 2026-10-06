package com.buyeong.umji.api.cart.service

import com.buyeong.umji.api.cart.model.AddCartItemCommand
import com.buyeong.umji.api.cart.model.CartItemState
import com.buyeong.umji.api.cart.model.CartItemView
import com.buyeong.umji.api.cart.model.CartState
import com.buyeong.umji.api.cart.model.CartView
import com.buyeong.umji.api.cart.model.SellableSku
import com.buyeong.umji.api.cart.model.UpdateCartItemCommand
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.cart.CartEntity
import com.buyeong.umji.api.persistence.jpa.cart.CartItemEntity
import com.buyeong.umji.api.persistence.jpa.cart.CartJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CartService(
    private val accounts: AccountJpaEntityService,
    private val carts: CartJpaEntityService,
    private val catalog: CatalogJpaEntityService,
) {
    @Transactional(readOnly = true)
    fun cart(accountId: UUID): CartView = load(accountId, lock = false)?.toView() ?: CartView(emptyList())

    @Transactional
    fun add(accountId: UUID, command: AddCartItemCommand): CartView {
        require((command.skuId != null) xor (command.salesOfferId != null)) { "SKU ID 또는 판매 오퍼 ID 중 하나만 지정해야 합니다." }
        val offer = command.salesOfferId?.let(catalog::salesOffer)
            ?: command.skuId?.let { catalog.salesOffer(command.channelCode.uppercase(), it) }
            ?: throw ItemNotFoundException("판매 오퍼를 찾을 수 없습니다.")
        val sku = offer.toSellable()
        require(sku.channelCode == command.channelCode.uppercase()) { "선택한 오퍼가 요청한 판매 채널과 일치하지 않습니다." }
        require(sku.salesStatus == ON_SALE) { "판매 중인 오퍼만 장바구니에 담을 수 있습니다." }
        val current = load(accountId, lock = true) ?: CartState(accountId, emptyList())
        require(current.items.isEmpty() || current.items.all { it.sku.channelCode == sku.channelCode }) {
            "장바구니에는 한 판매 채널의 상품만 담을 수 있습니다. 현재 상품을 주문하거나 비워 주세요."
        }
        val matching = current.items.firstOrNull { it.sku.id == sku.id }
        val updated = if (matching == null) {
            current.copy(items = current.items + CartItemState(null, sku, command.quantity))
        } else {
            current.copy(items = current.items.map { if (it === matching) it.copy(quantity = Math.addExact(it.quantity, command.quantity)) else it })
        }
        return save(updated).toView()
    }

    @Transactional
    fun update(accountId: UUID, itemId: UUID, command: UpdateCartItemCommand): CartView {
        val cart = load(accountId, lock = true) ?: throw ItemNotFoundException("장바구니를 찾을 수 없습니다.")
        requireItem(cart, itemId)
        return save(cart.copy(items = cart.items.map { if (it.id == itemId) it.copy(quantity = command.quantity) else it })).toView()
    }

    @Transactional
    fun remove(accountId: UUID, itemId: UUID) {
        val cart = load(accountId, lock = true) ?: throw ItemNotFoundException("장바구니를 찾을 수 없습니다.")
        requireItem(cart, itemId)
        save(cart.copy(items = cart.items.filterNot { it.id == itemId }))
    }

    @Transactional
    fun clearForCheckout(accountId: UUID) {
        val cart = load(accountId, lock = true) ?: return
        save(cart.copy(items = emptyList()))
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
                it.sku.unitsPerSale,
            )
        },
    )

    private fun load(accountId: UUID, lock: Boolean): CartState? {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val cart = if (lock) carts.findLocked(requireNotNull(account.id)) else carts.findWithItems(requireNotNull(account.id))
        return cart?.toState(accountId)
    }

    private fun save(state: CartState): CartState {
        val account = accounts.findByPublicId(state.accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val cart = carts.findLocked(requireNotNull(account.id)) ?: carts.create(account)
        val retainedIds = state.items.mapNotNull { it.id }.toSet()
        val newSkuIds = state.items.filter { it.id == null }.map { it.sku.id }.toSet()
        cart.items.filter { it.publicId !in retainedIds && it.sku.publicId !in newSkuIds }
            .toList().forEach(cart::remove)
        state.items.forEach { item ->
            val existing = item.id?.let { id -> cart.items.firstOrNull { it.publicId == id } }
            val entity = existing ?: CartItemEntity().apply {
                sku = catalog.sku(item.sku.id) ?: throw ItemNotFoundException("SKU를 찾을 수 없습니다.")
                salesOffer = catalog.salesOffer(item.sku.salesOfferId) ?: throw ItemNotFoundException("판매 오퍼를 찾을 수 없습니다.")
                cart.add(this)
            }
            entity.quantity = item.quantity
        }
        return carts.saveAndFlush(cart).toState(state.accountId)
    }

    private fun CartEntity.toState(accountId: UUID) = CartState(accountId, items.map { it.toState() })

    private fun CartItemEntity.toState() = CartItemState(
        id = publicId,
        sku = salesOffer.toSellable(),
        quantity = quantity,
    )

    private fun com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesOfferEntity.toSellable() = SellableSku(
        requireNotNull(productSku.publicId),
        requireNotNull(publicId),
        salesChannel.code,
        productSku.skuCode,
        productSku.product.name,
        productSku.name,
        salePrice,
        salesStatus,
        unitsPerSale,
    )

    private companion object {
        const val ON_SALE = "ON_SALE"
    }
}
