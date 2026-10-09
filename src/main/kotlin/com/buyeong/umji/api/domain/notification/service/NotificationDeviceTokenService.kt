package com.buyeong.umji.api.domain.notification.service

import com.buyeong.umji.api.domain.notification.dto.NotificationDeviceTokenRegistrationDto
import com.buyeong.umji.api.domain.notification.model.NotificationDevicePlatform
import com.buyeong.umji.api.persistence.jpa.notification.service.NotificationDeviceTokenJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Clock
import java.util.UUID

@Service
@Transactional
class NotificationDeviceTokenService(
    private val tokens: NotificationDeviceTokenJpaEntityService,
    private val clock: Clock,
) {
    fun register(
        accountId: UUID,
        platform: NotificationDevicePlatform,
        token: String,
    ): NotificationDeviceTokenRegistrationDto {
        require(token.isNotBlank() && token == token.trim() && token.length <= MAX_TOKEN_LENGTH) {
            "푸시 token은 1~4096자이며 앞뒤 공백을 포함할 수 없습니다."
        }
        if (platform == NotificationDevicePlatform.IOS_APNS) {
            require(APNS_TOKEN.matches(token)) { "APNs token은 64자리 16진수 문자열이어야 합니다." }
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return tokens.register(accountId, platform, token, hash, clock.instant())
    }

    fun revoke(accountId: UUID, tokenId: UUID) = tokens.revoke(accountId, tokenId)

    private companion object {
        const val MAX_TOKEN_LENGTH = 4096
        val APNS_TOKEN = Regex("^[A-Fa-f0-9]{64}$")
    }
}