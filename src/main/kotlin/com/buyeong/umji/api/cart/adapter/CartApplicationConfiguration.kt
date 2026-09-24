package com.buyeong.umji.api.cart.adapter

import com.buyeong.umji.api.cart.application.CartService
import com.buyeong.umji.api.cart.application.port.out.CartStorePort
import com.buyeong.umji.api.cart.application.port.out.SellableSkuQueryPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CartApplicationConfiguration {
    @Bean
    fun cartService(carts: CartStorePort, skus: SellableSkuQueryPort) = CartService(carts, skus)
}