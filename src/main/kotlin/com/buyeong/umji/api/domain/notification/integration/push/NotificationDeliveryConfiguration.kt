package com.buyeong.umji.api.domain.notification.integration.push

import com.buyeong.umji.api.domain.notification.integration.NotificationOutboxDispatchJob
import com.buyeong.umji.api.domain.notification.integration.apns.ApnsHttpPushProvider
import com.buyeong.umji.api.domain.notification.integration.fcm.FirebaseMessagingPushProvider
import com.buyeong.umji.api.domain.notification.service.NotificationOutboxWorker
import com.buyeong.umji.api.persistence.jpa.notification.service.NotificationDeviceTokenJpaEntityService
import com.buyeong.umji.api.persistence.jpa.notification.service.NotificationOutboxWorkerJpaEntityService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
@ConditionalOnExpression("\${umji.notification.fcm.enabled:false} or \${umji.notification.apns.enabled:false}")
class NotificationDeliveryConfiguration {
    @Bean
    fun notificationDeliveryService(
        deviceTokens: NotificationDeviceTokenJpaEntityService,
        fcm: ObjectProvider<FirebaseMessagingPushProvider>,
        apns: ObjectProvider<ApnsHttpPushProvider>,
    ): NotificationDeliveryService = NotificationDeliveryService(deviceTokens, fcm.ifAvailable, apns.ifAvailable)

    @Bean
    fun notificationOutboxWorker(
        outbox: NotificationOutboxWorkerJpaEntityService,
        delivery: NotificationDeliveryService,
    ) = NotificationOutboxWorker(outbox, delivery, Clock.systemUTC())

    @Bean
    fun notificationOutboxDispatchJob(worker: NotificationOutboxWorker) = NotificationOutboxDispatchJob(worker)
}