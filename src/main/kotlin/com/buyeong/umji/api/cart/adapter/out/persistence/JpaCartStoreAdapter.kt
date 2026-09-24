package com.buyeong.umji.api.cart.adapter.out.persistence

import com.buyeong.umji.api.cart.application.model.CartItemState
import com.buyeong.umji.api.cart.application.model.CartState
import com.buyeong.umji.api.cart.application.model.SellableSku
import com.buyeong.umji.api.cart.application.port.out.CartStorePort
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.cart.CartEntity
import com.buyeong.umji.api.persistence.jpa.cart.CartItemEntity
import com.buyeong.umji.api.persistence.jpa.cart.CartJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.CatalogJpaEntityService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class JpaCartStoreAdapter(
    private val accounts: AccountJpaEntityService,
    private val carts: CartJpaEntityService,
    private val catalog: CatalogJpaEntityService,
) : CartStorePort {
    @Transactional
    override fun find(accountId: UUID): CartState? {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        return carts.findLocked(requireNotNull(account.id))?.toState(accountId)
    }

    @Transactional
    override fun save(state: CartState): CartState {
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
                cart.add(this)
            }
            entity.quantity = item.quantity
        }
        return carts.saveAndFlush(cart).toState(state.accountId)
    }

    private fun CartEntity.toState(accountId: UUID) = CartState(accountId, items.map { it.toState() })

    private fun CartItemEntity.toState() = CartItemState(
        id = publicId,
        sku = SellableSku(requireNotNull(sku.publicId), sku.skuCode, sku.product.name, sku.name, sku.salePrice, sku.salesStatus),
        quantity = quantity,
    )
}