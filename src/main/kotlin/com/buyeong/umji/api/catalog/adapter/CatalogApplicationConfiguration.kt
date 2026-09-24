package com.buyeong.umji.api.catalog.adapter

import com.buyeong.umji.api.catalog.application.CatalogService
import com.buyeong.umji.api.catalog.application.port.out.CatalogReadPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CatalogApplicationConfiguration {
    @Bean
    fun catalogService(catalog: CatalogReadPort) = CatalogService(catalog)
}