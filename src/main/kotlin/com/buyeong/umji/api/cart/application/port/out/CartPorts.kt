package com.buyeong.umji.api.cart.application.port.out

import com.buyeong.umji.api.cart.application.model.CartState
import com.buyeong.umji.api.cart.application.model.SellableSku
import java.util.UUID

interface CartStorePort {
    fun find(accountId: UUID): CartState?
    fun save(state: CartState): CartState
}

interface SellableSkuQueryPort {
    fun find(skuId: UUID): SellableSku?
}
