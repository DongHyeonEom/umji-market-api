package com.buyeong.umji.api.notification.dto

import com.buyeong.umji.api.notification.model.NotificationDevicePlatform
import java.util.UUID

data class NotificationDeviceRecipientDto(val id: UUID, val platform: NotificationDevicePlatform, val token: String)