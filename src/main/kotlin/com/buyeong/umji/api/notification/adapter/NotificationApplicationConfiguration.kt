package com.buyeong.umji.api.notification.adapter

import net.ttddyy.dsproxy.listener.logging.LoggingFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NotificationApplicationConfiguration {
    @Bean
    fun notificationDeviceTokenQueryLoggingFilter(): LoggingFilter =
        LoggingFilter { _, queries -> queries.none { it.query.contains("notification_device_token", ignoreCase = true) } }

    @Bean
    fun notificationClock() = java.time.Clock.systemUTC()
}
