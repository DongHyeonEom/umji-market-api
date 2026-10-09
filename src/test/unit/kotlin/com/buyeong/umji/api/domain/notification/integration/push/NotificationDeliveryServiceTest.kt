package com.buyeong.umji.api.domain.notification.integration.push

import com.buyeong.umji.api.domain.notification.dto.NotificationDeviceRecipientDto
import com.buyeong.umji.api.domain.notification.dto.NotificationEventDto
import com.buyeong.umji.api.domain.notification.dto.NotificationEventType
import com.buyeong.umji.api.domain.notification.dto.NotificationMessageDto
import com.buyeong.umji.api.domain.notification.dto.NotificationPermanentFailureDto
import com.buyeong.umji.api.domain.notification.dto.NotificationProviderAcceptedDto
import com.buyeong.umji.api.domain.notification.dto.NotificationProviderInvalidTokenDto
import com.buyeong.umji.api.domain.notification.dto.NotificationProviderRetryableFailureDto
import com.buyeong.umji.api.domain.notification.dto.NotificationRetryableFailureDto
import com.buyeong.umji.api.domain.notification.dto.NotificationSentDto
import com.buyeong.umji.api.domain.notification.dto.toMessage
import com.buyeong.umji.api.domain.notification.integration.apns.ApnsHttpPushProvider
import com.buyeong.umji.api.domain.notification.integration.fcm.FirebaseMessagingPushProvider
import com.buyeong.umji.api.domain.notification.model.NotificationDevicePlatform
import com.buyeong.umji.api.persistence.jpa.notification.service.NotificationDeviceTokenJpaEntityService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class NotificationDeliveryServiceTest : DescribeSpec({
    val orderId = UUID.randomUUID()
    val event = NotificationEventDto(UUID.randomUUID(), NotificationEventType.PAYMENT_STATUS_CHANGED, orderId, "account=private")
    val androidRecipient = NotificationDeviceRecipientDto(UUID.randomUUID(), NotificationDevicePlatform.ANDROID_FCM, "fcm-token")
    val iosRecipient = NotificationDeviceRecipientDto(UUID.randomUUID(), NotificationDevicePlatform.IOS_APNS, "a".repeat(64))

    describe("기기별 push 전달") {
        it("주문 계정의 활성 기기에 일반 문구와 안전한 앱 이동 data만 전달한다") {
            val tokens = mockk<NotificationDeviceTokenJpaEntityService>()
            every { tokens.activeRecipientsForOrder(orderId) } returns listOf(androidRecipient)
            var sentMessage: NotificationMessageDto? = null
            val fcm = mockk<FirebaseMessagingPushProvider>()
            every { fcm.send(any(), any()) } answers {
                sentMessage = secondArg()
                NotificationProviderAcceptedDto
            }

            NotificationDeliveryService(tokens, fcm, null).deliver(event) shouldBe NotificationSentDto

            sentMessage shouldBe event.toMessage()
            sentMessage!!.title shouldBe "결제 안내"
            sentMessage!!.body shouldBe "주문 결제 상태가 변경되었습니다."
            sentMessage!!.data shouldBe mapOf("notificationType" to "PAYMENT_STATUS_CHANGED", "orderId" to orderId.toString())
            sentMessage!!.toString().contains("account=private") shouldBe false
            verify(exactly = 1) { tokens.activeRecipientsForOrder(orderId) }
        }

        it("활성 기기가 없으면 provider를 호출하지 않고 성공으로 처리한다") {
            val tokens = mockk<NotificationDeviceTokenJpaEntityService>()
            every { tokens.activeRecipientsForOrder(orderId) } returns emptyList()
            NotificationDeliveryService(tokens, null, null).deliver(event) shouldBe NotificationSentDto
        }

        it("무효 token은 비활성화하고 나머지 유효 전달은 성공 처리한다") {
            val tokens = mockk<NotificationDeviceTokenJpaEntityService>()
            every { tokens.activeRecipientsForOrder(orderId) } returns listOf(androidRecipient, iosRecipient)
            every { tokens.deactivate(androidRecipient.id) } returns Unit
            val fcm = mockk<FirebaseMessagingPushProvider>()
            val apns = mockk<ApnsHttpPushProvider>()
            every { fcm.send(any(), any()) } returns NotificationProviderInvalidTokenDto
            every { apns.send(any(), any()) } returns NotificationProviderAcceptedDto

            NotificationDeliveryService(tokens, fcm, apns).deliver(event) shouldBe NotificationSentDto

            verify(exactly = 1) { tokens.deactivate(androidRecipient.id) }
        }

        it("provider 일시 실패는 outbox 재시도 결과로 전달한다") {
            val tokens = mockk<NotificationDeviceTokenJpaEntityService>()
            every { tokens.activeRecipientsForOrder(orderId) } returns listOf(androidRecipient)
            val fcm = mockk<FirebaseMessagingPushProvider>()
            every { fcm.send(any(), any()) } returns NotificationProviderRetryableFailureDto("FCM_UNAVAILABLE")

            NotificationDeliveryService(tokens, fcm, null).deliver(event) shouldBe
                NotificationRetryableFailureDto("FCM_UNAVAILABLE")
        }

        it("provider가 설정되지 않은 platform은 영구 실패로 처리한다") {
            val tokens = mockk<NotificationDeviceTokenJpaEntityService>()
            every { tokens.activeRecipientsForOrder(orderId) } returns listOf(iosRecipient)

            NotificationDeliveryService(tokens, null, null).deliver(event) shouldBe
                NotificationPermanentFailureDto("PROVIDER_NOT_CONFIGURED")
        }
    }
})