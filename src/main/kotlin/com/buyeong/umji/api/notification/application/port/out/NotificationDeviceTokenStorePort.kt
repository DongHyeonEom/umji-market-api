package com.buyeong.umji.api.notification.application.port.out

import com.buyeong.umji.api.notification.application.model.NotificationDevicePlatform
import com.buyeong.umji.api.notification.application.model.NotificationDeviceRecipient
import com.buyeong.umji.api.notification.application.model.NotificationDeviceTokenRegistration
import java.time.Instant
import java.util.UUID

interface NotificationDeviceTokenStorePort {
    fun register(
        accountId: UUID,
        platform: NotificationDevicePlatform,
        token: String,
        tokenHash: ByteArray,
        registeredAt: Instant,
    ): NotificationDeviceTokenRegistration

    fun revoke(accountId: UUID, tokenId: UUID)

    fun activeRecipientsForOrder(orderId: UUID): List<NotificationDeviceRecipient>

    fun deactivate(tokenId: UUID)
}