package com.buyeong.umji.api.notification.application.model

import java.time.Instant
import java.util.UUID

enum class NotificationDevicePlatform {
    ANDROID_FCM,
    IOS_APNS,
}

data class NotificationDeviceTokenRegistration(
    val id: UUID,
    val platform: NotificationDevicePlatform,
    val registeredAt: Instant,
)