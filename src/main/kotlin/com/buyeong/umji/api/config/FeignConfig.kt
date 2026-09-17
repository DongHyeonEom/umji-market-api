package com.buyeong.umji.api.config

import feign.RequestInterceptor
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FeignConfig {
    @Bean
    fun requestInterceptor(): RequestInterceptor? =
        RequestInterceptor { requestTemplate ->
            val traceId = MDC.get("traceId")
            if (traceId != null) {
                requestTemplate.header("traceId", traceId)
            }
        }
}