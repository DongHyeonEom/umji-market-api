package com.buyeong.umji.api.domain.cart.service

import com.buyeong.umji.api.domain.cart.dto.AddCartItemCommandDto
import com.buyeong.umji.api.domain.cart.dto.CartItemStateDto
import com.buyeong.umji.api.domain.cart.dto.CartItemViewDto
import com.buyeong.umji.api.domain.cart.dto.CartStateDto
import com.buyeong.umji.api.domain.cart.dto.CartViewDto
import com.buyeong.umji.api.domain.cart.dto.SellableSkuDto
import com.buyeong.umji.api.domain.cart.dto.UpdateCartItemCommandDto
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationJpaEntityService
import com.buyeong.umji.api.persistence.jpa.cart.entity.CartEntity
import com.buyeong.umji.api.persistence.jpa.cart.entity.CartItemEntity
import com.buyeong.umji.api.persistence.jpa.cart.service.CartJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesOfferEntity
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CartService(
    private val accounts: AccountJpaEntityService,
    private val carts: CartJpaEntityService,
    private val catalog: CatalogJpaEntityService,
    private val organizations: OrganizationJpaEntityService,
) {
    @Transactional(readOnly = true)
    fun cart(accountId: UUID): CartViewDto =
        (load(accountId, lock = false) ?: CartStateDto(accountId, emptyList())).toView(organizations.buyerTypeForAccountPublicId(accountId))

    @Transactional
    fun add(accountId: UUID, command: AddCartItemCommandDto): CartViewDto {
        require((command.skuId != null) xor (command.salesOfferId != null)) { "SKU ID 또는 판매 오퍼 ID 중 하나만 지정해야 합니다." }
        organizations.buyerTypeForAccountPublicId(accountId)
        require(command.channelCode == null || command.channelCode.uppercase() in setOf(WHOLESALE, RETAIL)) { "유효하지 않은 판매 채널입니다." }
        val requestedOffer = command.salesOfferId?.let(catalog::salesOffer)
            ?: command.skuId?.let { catalog.salesOffer(WHOLESALE, it) }
            ?: throw ItemNotFoundException("판매 오퍼를 찾을 수 없습니다.")
        val selectedSkuId = requireNotNull(requestedOffer.productSku.publicId)
        val offer = requestedOffer.organization?.publicId?.let { catalog.salesOffer(WHOLESALE, selectedSkuId, it) }
            ?: catalog.salesOffer(WHOLESALE, selectedSkuId)
            ?: requestedOffer
        val sku = offer.toSellable()
        require(sku.channelCode == WHOLESALE) { "통합 카탈로그 판매 오퍼를 찾을 수 없습니다." }
        require(sku.salesStatus == ON_SALE) { "판매 중인 오퍼만 장바구니에 담을 수 있습니다." }
        val current = load(accountId, lock = true) ?: CartStateDto(accountId, emptyList())
        val matching = current.items.firstOrNull { it.sku.salesOfferId == sku.salesOfferId }
        val updated = if (matching == null) {
            current.copy(items = current.items + CartItemStateDto(null, sku, command.quantity))
        } else {
            current.copy(items = current.items.map { if (it === matching) it.copy(quantity = Math.addExact(it.quantity, command.quantity)) else it })
        }
        return save(updated).toView(organizations.buyerTypeForAccountPublicId(accountId))
    }

    @Transactional
    fun update(accountId: UUID, itemId: UUID, command: UpdateCartItemCommandDto): CartViewDto {
        val cart = load(accountId, lock = true) ?: throw ItemNotFoundException("장바구니를 찾을 수 없습니다.")
        organizations.buyerTypeForAccountPublicId(accountId)
        requireItem(cart, itemId)
        return save(cart.copy(items = cart.items.map { if (it.id == itemId) it.copy(quantity = command.quantity) else it }))
            .toView(organizations.buyerTypeForAccountPublicId(accountId))
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

    private fun requireItem(cart: CartStateDto, itemId: UUID) =
        cart.items.firstOrNull { it.id == itemId } ?: throw ItemNotFoundException("장바구니 항목을 찾을 수 없습니다.")

    private fun CartStateDto.toView(buyerType: String) = CartViewDto(
        items.map { item ->
            val productQuantity = items.asSequence()
                .filter { it.sku.productId == item.sku.productId }
                .sumOf { it.quantity.toLong() }
            val wholesaleApplied = buyerType == WHOLESALE_BUSINESS && productQuantity >= item.sku.wholesaleMinimumQuantity
            CartItemViewDto(
                requireNotNull(item.id),
                item.sku.id,
                item.sku.code,
                item.sku.productName,
                item.sku.name,
                item.quantity,
                if (wholesaleApplied) item.sku.wholesaleSalePrice else item.sku.retailSalePrice,
                item.sku.retailSalePrice,
                item.sku.wholesaleSalePrice,
                item.sku.wholesaleMinimumQuantity,
                if (wholesaleApplied) WHOLESALE else RETAIL,
                item.sku.salesStatus,
                item.sku.salesOfferId,
                item.sku.channelCode,
                item.sku.unitsPerSale,
                item.sku.sellerOrganizationId,
            )
        },
    )

    private fun load(accountId: UUID, lock: Boolean): CartStateDto? {
        val account = accounts.findByPublicId(accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val cart = if (lock) carts.findLocked(requireNotNull(account.id)) else carts.findWithItems(requireNotNull(account.id))
        return cart?.toState(accountId)
    }

    private fun save(state: CartStateDto): CartStateDto {
        val account = accounts.findByPublicId(state.accountId) ?: throw ItemNotFoundException("계정을 찾을 수 없습니다.")
        val cart = carts.findLocked(requireNotNull(account.id)) ?: carts.create(account)
        val retainedIds = state.items.mapNotNull { it.id }.toSet()
        val newOfferIds = state.items.filter { it.id == null }.map { it.sku.salesOfferId }.toSet()
        cart.items.filter { it.publicId !in retainedIds && it.salesOffer.publicId !in newOfferIds }
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

    private fun CartEntity.toState(accountId: UUID) = CartStateDto(accountId, items.map { it.toState() })

    private fun CartItemEntity.toState() = CartItemStateDto(
        id = publicId,
        sku = salesOffer.toSellable(),
        quantity = quantity,
    )

    private fun com.buyeong.umji.api.persistence.jpa.catalog.entity.SalesOfferEntity.toSellable() = SellableSkuDto(
        requireNotNull(productSku.publicId),
        requireNotNull(productSku.product.publicId),
        requireNotNull(publicId),
        WHOLESALE,
        productSku.skuCode,
        productSku.product.name,
        productSku.name,
        retailSalePrice,
        retailSalePrice,
        wholesaleSalePrice,
        productSku.product.wholesaleMinimumQuantity,
        salesStatus,
        unitsPerSale,
        organization?.publicId,
    )

    private companion object {
        const val ON_SALE = "ON_SALE"
        const val WHOLESALE = "WHOLESALE"
        const val RETAIL = "RETAIL"
        const val WHOLESALE_BUSINESS = "WHOLESALE_BUSINESS"
    }
}
