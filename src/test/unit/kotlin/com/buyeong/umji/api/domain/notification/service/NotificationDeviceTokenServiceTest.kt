package com.buyeong.umji.api.domain.notification.service

import com.buyeong.umji.api.domain.notification.model.NotificationDevicePlatform
import com.buyeong.umji.api.domain.notification.model.NotificationDeviceTokenRegistration
import com.buyeong.umji.api.persistence.jpa.notification.service.NotificationDeviceTokenJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class NotificationDeviceTokenServiceTest : DescribeSpec({
    val accountId = UUID.randomUUID()
    val tokenId = UUID.randomUUID()
    val registeredAt = Instant.parse("2026-10-05T00:00:00Z")
    val store = mockk<NotificationDeviceTokenJpaEntityService>()
    val service = NotificationDeviceTokenService(store, Clock.fixed(registeredAt, ZoneOffset.UTC))

    beforeTest { clearMocks(store) }

    describe("기기 token 등록·해제") {
        it("Android token의 SHA-256과 등록 시각을 저장소에 전달한다") {
            val token = "fcm-device-token"
            val expectedHash = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
            val registration = NotificationDeviceTokenRegistration(tokenId, NotificationDevicePlatform.ANDROID_FCM, registeredAt)
            every { store.register(accountId, NotificationDevicePlatform.ANDROID_FCM, token, expectedHash, registeredAt) } returns registration

            service.register(accountId, NotificationDevicePlatform.ANDROID_FCM, token) shouldBe registration

            verify(exactly = 1) { store.register(accountId, NotificationDevicePlatform.ANDROID_FCM, token, expectedHash, registeredAt) }
        }

        it("64자리 16진수 APNs token을 허용한다") {
            val token = "aB12".repeat(16)
            every { store.register(any(), any(), any(), any(), any()) } returns
                NotificationDeviceTokenRegistration(tokenId, NotificationDevicePlatform.IOS_APNS, registeredAt)

            service.register(accountId, NotificationDevicePlatform.IOS_APNS, token).platform shouldBe NotificationDevicePlatform.IOS_APNS
        }

        it("Android token은 1자와 4096자를 허용하지만 공백·빈 값·초과 길이는 거부한다") {
            every { store.register(any(), any(), any(), any(), any()) } returns
                NotificationDeviceTokenRegistration(tokenId, NotificationDevicePlatform.ANDROID_FCM, registeredAt)

            service.register(accountId, NotificationDevicePlatform.ANDROID_FCM, "x").id shouldBe tokenId
            service.register(accountId, NotificationDevicePlatform.ANDROID_FCM, "x".repeat(4096)).id shouldBe tokenId
            listOf("", " ", " token", "token ", "x".repeat(4097)).forEach { token ->
                shouldThrow<IllegalArgumentException> { service.register(accountId, NotificationDevicePlatform.ANDROID_FCM, token) }
            }
            verify(exactly = 2) { store.register(any(), NotificationDevicePlatform.ANDROID_FCM, any(), any(), registeredAt) }
        }

        it("APNs token은 64자리 16진수가 아니면 저장하지 않는다") {
            listOf("a".repeat(63), "a".repeat(65), "g".repeat(64)).forEach { token ->
                shouldThrow<IllegalArgumentException> { service.register(accountId, NotificationDevicePlatform.IOS_APNS, token) }
            }
            verify(exactly = 0) { store.register(any(), any(), any(), any(), any()) }
        }

        it("해제 요청은 인증 계정과 공개 token ID를 저장소에 전달한다") {
            every { store.revoke(accountId, tokenId) } returns Unit

            service.revoke(accountId, tokenId)

            verify(exactly = 1) { store.revoke(accountId, tokenId) }
        }
    }
})