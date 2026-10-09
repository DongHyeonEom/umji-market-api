package com.buyeong.umji.api.domain.notification.dto

import com.buyeong.umji.api.domain.notification.model.NotificationDevicePlatform
import java.util.UUID

data class NotificationDeviceRecipientDto(val id: UUID, val platform: NotificationDevicePlatform, val token: String)