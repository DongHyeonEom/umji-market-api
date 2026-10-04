package com.buyeong.umji.api.notification.adapter.out.push

import com.buyeong.umji.api.notification.application.model.NotificationDevicePlatform
import com.buyeong.umji.api.notification.application.model.NotificationDeviceRecipient
import com.buyeong.umji.api.notification.application.model.NotificationEvent
import com.buyeong.umji.api.notification.application.model.NotificationEventType
import com.buyeong.umji.api.notification.application.model.NotificationMessage
import com.buyeong.umji.api.notification.application.model.toMessage
import com.buyeong.umji.api.notification.application.port.out.NotificationDeliveryResult
import com.buyeong.umji.api.notification.application.port.out.NotificationDeviceTokenStorePort
import com.buyeong.umji.api.notification.application.port.out.NotificationProviderResult
import com.buyeong.umji.api.notification.application.port.out.NotificationPushProviderPort
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class NotificationDeliveryAdapterTest : DescribeSpec({
    val orderId = UUID.randomUUID()
    val event = NotificationEvent(UUID.randomUUID(), NotificationEventType.PAYMENT_STATUS_CHANGED, orderId, "account=private")
    val androidRecipient = NotificationDeviceRecipient(UUID.randomUUID(), NotificationDevicePlatform.ANDROID_FCM, "fcm-token")
    val iosRecipient = NotificationDeviceRecipient(UUID.randomUUID(), NotificationDevicePlatform.IOS_APNS, "a".repeat(64))

    describe("기기별 push 전달") {
        it("주문 계정의 활성 기기에 일반 문구와 안전한 앱 이동 data만 전달한다") {
            val tokens = mockk<NotificationDeviceTokenStorePort>()
            every { tokens.activeRecipientsForOrder(orderId) } returns listOf(androidRecipient)
            var sentMessage: NotificationMessage? = null
            val fcm = provider(NotificationDevicePlatform.ANDROID_FCM) { _, message ->
                sentMessage = message
                NotificationProviderResult.Accepted
            }

            NotificationDeliveryAdapter(tokens, listOf(fcm)).deliver(event) shouldBe NotificationDeliveryResult.Sent

            sentMessage shouldBe event.toMessage()
            sentMessage!!.title shouldBe "결제 안내"
            sentMessage!!.body shouldBe "주문 결제 상태가 변경되었습니다."
            sentMessage!!.data shouldBe mapOf("notificationType" to "PAYMENT_STATUS_CHANGED", "orderId" to orderId.toString())
            sentMessage!!.toString().contains("account=private") shouldBe false
            verify(exactly = 1) { tokens.activeRecipientsForOrder(orderId) }
        }

        it("활성 기기가 없으면 provider를 호출하지 않고 성공으로 처리한다") {
            val tokens = mockk<NotificationDeviceTokenStorePort>()
            val fcm = provider(NotificationDevicePlatform.ANDROID_FCM) { _, _ -> error("must not send") }
            every { tokens.activeRecipientsForOrder(orderId) } returns emptyList()

            NotificationDeliveryAdapter(tokens, listOf(fcm)).deliver(event) shouldBe NotificationDeliveryResult.Sent
        }

        it("무효 token은 비활성화하고 나머지 유효 전달은 성공 처리한다") {
            val tokens = mockk<NotificationDeviceTokenStorePort>()
            every { tokens.activeRecipientsForOrder(orderId) } returns listOf(androidRecipient, iosRecipient)
            every { tokens.deactivate(androidRecipient.id) } returns Unit
            val fcm = provider(NotificationDevicePlatform.ANDROID_FCM) { _, _ -> NotificationProviderResult.InvalidToken }
            val apns = provider(NotificationDevicePlatform.IOS_APNS) { _, _ -> NotificationProviderResult.Accepted }

            NotificationDeliveryAdapter(tokens, listOf(fcm, apns)).deliver(event) shouldBe NotificationDeliveryResult.Sent

            verify(exactly = 1) { tokens.deactivate(androidRecipient.id) }
        }

        it("provider 일시 실패는 outbox 재시도 결과로 전달한다") {
            val tokens = mockk<NotificationDeviceTokenStorePort>()
            every { tokens.activeRecipientsForOrder(orderId) } returns listOf(androidRecipient)
            val fcm = provider(NotificationDevicePlatform.ANDROID_FCM) { _, _ ->
                NotificationProviderResult.RetryableFailure("FCM_UNAVAILABLE")
            }

            NotificationDeliveryAdapter(tokens, listOf(fcm)).deliver(event) shouldBe
                NotificationDeliveryResult.RetryableFailure("FCM_UNAVAILABLE")
        }

        it("provider가 설정되지 않은 platform은 영구 실패로 처리한다") {
            val tokens = mockk<NotificationDeviceTokenStorePort>()
            every { tokens.activeRecipientsForOrder(orderId) } returns listOf(iosRecipient)

            NotificationDeliveryAdapter(tokens, emptyList()).deliver(event) shouldBe
                NotificationDeliveryResult.PermanentFailure("PROVIDER_NOT_CONFIGURED")
        }
    }
})

private fun provider(
    platform: NotificationDevicePlatform,
    send: (NotificationDeviceRecipient, NotificationMessage) -> NotificationProviderResult,
) = object : NotificationPushProviderPort {
    override val platform = platform

    override fun send(recipient: NotificationDeviceRecipient, message: NotificationMessage): NotificationProviderResult = send(recipient, message)
}