package com.buyeong.umji.api.cart.persistence

import com.buyeong.umji.api.account.persistence.AccountEntity
import com.buyeong.umji.api.persistence.jpa.entity.backbone.DomainPublicEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "cart")
class CartEntity : DomainPublicEntity() {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    lateinit var account: AccountEntity

    @OneToMany(mappedBy = "cart", cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<CartItemEntity> = mutableListOf()

    fun add(item: CartItemEntity) {
        item.cart = this
        items.add(item)
    }

    fun remove(item: CartItemEntity) {
        items.remove(item)
    }
}
