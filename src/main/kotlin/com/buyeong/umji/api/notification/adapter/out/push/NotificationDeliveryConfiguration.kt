package com.buyeong.umji.api.notification.adapter.out.push

import com.buyeong.umji.api.notification.adapter.NotificationOutboxDispatchJob
import com.buyeong.umji.api.notification.application.NotificationOutboxWorker
import com.buyeong.umji.api.notification.application.port.out.NotificationDeliveryPort
import com.buyeong.umji.api.notification.application.port.out.NotificationDeviceTokenStorePort
import com.buyeong.umji.api.notification.application.port.out.NotificationOutboxWorkerPort
import com.buyeong.umji.api.notification.application.port.out.NotificationPushProviderPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
@ConditionalOnExpression("\${umji.notification.fcm.enabled:false} or \${umji.notification.apns.enabled:false}")
class NotificationDeliveryConfiguration {
    @Bean
    fun notificationDeliveryPort(
        deviceTokens: NotificationDeviceTokenStorePort,
        providers: List<NotificationPushProviderPort>,
    ): NotificationDeliveryPort = NotificationDeliveryAdapter(deviceTokens, providers)

    @Bean
    fun notificationOutboxWorker(
        outbox: NotificationOutboxWorkerPort,
        delivery: NotificationDeliveryPort,
    ) = NotificationOutboxWorker(outbox, delivery, Clock.systemUTC())

    @Bean
    fun notificationOutboxDispatchJob(worker: NotificationOutboxWorker) = NotificationOutboxDispatchJob(worker)
}