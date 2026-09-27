package com.buyeong.umji.api.payment.adapter

import com.buyeong.umji.api.payment.application.PaymentService
import com.buyeong.umji.api.payment.application.port.out.PaymentInventoryPort
import com.buyeong.umji.api.payment.application.port.out.PaymentStorePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PaymentApplicationConfiguration {
    @Bean
    fun paymentService(payments: PaymentStorePort, inventory: PaymentInventoryPort) = PaymentService(payments, inventory)
}
