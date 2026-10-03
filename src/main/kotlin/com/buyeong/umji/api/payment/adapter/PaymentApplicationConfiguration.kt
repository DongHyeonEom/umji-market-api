package com.buyeong.umji.api.payment.adapter

import com.buyeong.umji.api.notification.application.port.`in`.NotificationEventUseCase
import com.buyeong.umji.api.payment.application.PaymentService
import com.buyeong.umji.api.payment.application.port.out.PaymentStorePort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PaymentApplicationConfiguration {
    @Bean
    fun paymentService(payments: PaymentStorePort, notifications: NotificationEventUseCase) = PaymentService(payments, notifications)
}