package com.buyeong.umji.api.notification.application

import com.buyeong.umji.api.notification.application.model.NotificationDevicePlatform
import com.buyeong.umji.api.notification.application.model.NotificationDeviceTokenRegistration
import com.buyeong.umji.api.notification.application.port.`in`.NotificationDeviceTokenUseCase
import com.buyeong.umji.api.notification.application.port.out.NotificationDeviceTokenStorePort
import java.security.MessageDigest
import java.time.Clock
import java.util.UUID

class NotificationDeviceTokenService(
    private val tokens: NotificationDeviceTokenStorePort,
    private val clock: Clock,
) : NotificationDeviceTokenUseCase {
    override fun register(
        accountId: UUID,
        platform: NotificationDevicePlatform,
        token: String,
    ): NotificationDeviceTokenRegistration {
        require(token.isNotBlank() && token == token.trim() && token.length <= MAX_TOKEN_LENGTH) {
            "푸시 token은 1~4096자이며 앞뒤 공백을 포함할 수 없습니다."
        }
        if (platform == NotificationDevicePlatform.IOS_APNS) {
            require(APNS_TOKEN.matches(token)) { "APNs token은 64자리 16진수 문자열이어야 합니다." }
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return tokens.register(accountId, platform, token, hash, clock.instant())
    }

    override fun revoke(accountId: UUID, tokenId: UUID) = tokens.revoke(accountId, tokenId)

    private companion object {
        const val MAX_TOKEN_LENGTH = 4096
        val APNS_TOKEN = Regex("^[A-Fa-f0-9]{64}$")
    }
}