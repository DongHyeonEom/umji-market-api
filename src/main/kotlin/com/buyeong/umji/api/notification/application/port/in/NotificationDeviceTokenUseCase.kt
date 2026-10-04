package com.buyeong.umji.api.notification.application.port.`in`

import com.buyeong.umji.api.notification.application.model.NotificationDevicePlatform
import com.buyeong.umji.api.notification.application.model.NotificationDeviceTokenRegistration
import java.util.UUID

interface NotificationDeviceTokenUseCase {
    fun register(accountId: UUID, platform: NotificationDevicePlatform, token: String): NotificationDeviceTokenRegistration

    fun revoke(accountId: UUID, tokenId: UUID)
}