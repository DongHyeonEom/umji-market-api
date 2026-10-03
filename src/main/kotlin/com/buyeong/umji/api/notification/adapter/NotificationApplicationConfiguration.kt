package com.buyeong.umji.api.notification.adapter

import com.buyeong.umji.api.notification.application.NotificationEventService
import com.buyeong.umji.api.notification.application.port.out.NotificationOutboxPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NotificationApplicationConfiguration {
    @Bean
    fun notificationEventUseCase(outbox: NotificationOutboxPort) = NotificationEventService(outbox)
}