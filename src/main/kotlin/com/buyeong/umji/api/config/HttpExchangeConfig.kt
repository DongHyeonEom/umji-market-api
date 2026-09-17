package com.buyeong.umji.api.config

import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HttpExchangeConfig {
    @Bean
    fun httpExchangeRepository(): InMemoryHttpExchangeRepository = InMemoryHttpExchangeRepository()
}