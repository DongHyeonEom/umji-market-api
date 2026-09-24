package com.buyeong.umji.api.persistence.jpa.cart

import com.buyeong.umji.api.persistence.jpa.account.AccountEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CartJpaEntityService(
    private val carts: CartRepository,
) {
    fun findWithItems(accountId: Long): CartEntity? = carts.findWithItemsByAccountId(accountId)

    @Transactional fun findLocked(accountId: Long): CartEntity? = carts.findLockedByAccountId(accountId)

    @Transactional fun saveAndFlush(cart: CartEntity): CartEntity = carts.saveAndFlush(cart)

    @Transactional fun create(account: AccountEntity): CartEntity = carts.save(CartEntity().apply { this.account = account })
}