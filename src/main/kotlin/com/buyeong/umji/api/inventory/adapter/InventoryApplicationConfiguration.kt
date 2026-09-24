package com.buyeong.umji.api.inventory.adapter

import com.buyeong.umji.api.inventory.application.InventoryService
import com.buyeong.umji.api.inventory.application.port.out.InventoryStorePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InventoryApplicationConfiguration {
    @Bean
    fun inventoryService(store: InventoryStorePort) = InventoryService(store)
}