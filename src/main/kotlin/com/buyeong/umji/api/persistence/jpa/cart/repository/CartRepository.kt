package com.buyeong.umji.api.persistence.jpa.cart

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CartRepository : JpaRepository<CartEntity, Long> {
    @Query("select distinct cart from CartEntity cart left join fetch cart.items item left join fetch item.sku sku left join fetch sku.product where cart.account.id = :accountId")
    fun findWithItemsByAccountId(@Param("accountId") accountId: Long): CartEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cart from CartEntity cart where cart.account.id = :accountId")
    fun findLockedByAccountId(@Param("accountId") accountId: Long): CartEntity?
}