package com.buyeong.umji.api.notification.dto

import com.buyeong.umji.api.notification.model.NotificationDevicePlatform
import java.time.Instant
import java.util.UUID

data class NotificationDeviceTokenRegistrationDto(
    val id: UUID,
    val platform: NotificationDevicePlatform,
    val registeredAt: Instant,
)