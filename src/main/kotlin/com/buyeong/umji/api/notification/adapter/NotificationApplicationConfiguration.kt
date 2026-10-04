package com.buyeong.umji.api.notification.adapter

import com.buyeong.umji.api.notification.application.NotificationEventService
import com.buyeong.umji.api.notification.application.NotificationOutboxWorker
import com.buyeong.umji.api.notification.application.port.out.NotificationDeliveryPort
import com.buyeong.umji.api.notification.application.port.out.NotificationOutboxPort
import com.buyeong.umji.api.notification.application.port.out.NotificationOutboxWorkerPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class NotificationApplicationConfiguration {
    @Bean
    fun notificationEventUseCase(outbox: NotificationOutboxPort) = NotificationEventService(outbox)

    @Bean
    @ConditionalOnBean(NotificationDeliveryPort::class)
    fun notificationOutboxWorker(
        outbox: NotificationOutboxWorkerPort,
        delivery: NotificationDeliveryPort,
    ) = NotificationOutboxWorker(outbox, delivery, Clock.systemUTC())
}