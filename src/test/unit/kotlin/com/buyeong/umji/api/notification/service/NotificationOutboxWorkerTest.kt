package com.buyeong.umji.api.notification.service

import com.buyeong.umji.api.notification.integration.push.NotificationDeliveryService
import com.buyeong.umji.api.notification.model.ClaimedNotification
import com.buyeong.umji.api.notification.model.NotificationDeliveryResult
import com.buyeong.umji.api.notification.model.NotificationEvent
import com.buyeong.umji.api.notification.model.NotificationEventType
import com.buyeong.umji.api.persistence.jpa.notification.service.NotificationOutboxWorkerJpaEntityService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class NotificationOutboxWorkerTest : DescribeSpec({
    val now = Instant.parse("2026-10-05T00:00:00Z")
    val clock = Clock.fixed(now, ZoneOffset.UTC)
    val event = NotificationEvent(UUID.randomUUID(), NotificationEventType.ORDER_CREATED, UUID.randomUUID(), "sensitive detail")

    describe("outbox batch dispatch") {
        it("성공 결과를 발송 완료로 기록한다") {
            val outbox = mockk<NotificationOutboxWorkerJpaEntityService>()
            val delivery = mockk<NotificationDeliveryService>()
            every { outbox.claimBatch(now, Duration.ofMinutes(2), 100) } returns listOf(ClaimedNotification(event, 1))
            every { delivery.deliver(event) } returns NotificationDeliveryResult.Sent
            every { outbox.markSent(event.id, now) } returns Unit

            NotificationOutboxWorker(outbox, delivery, clock).dispatchBatch() shouldBe 1

            verify(exactly = 1) { outbox.markSent(event.id, now) }
        }

        it("일시 실패를 시도 횟수에 해당하는 간격으로 다시 예약하고 오류 코드를 정규화한다") {
            listOf(1 to 1L, 2 to 5L, 3 to 15L).forEach { (attempt, minutes) ->
                val outbox = mockk<NotificationOutboxWorkerJpaEntityService>()
                val delivery = mockk<NotificationDeliveryService>()
                every { outbox.claimBatch(now, Duration.ofMinutes(2), 100) } returns listOf(ClaimedNotification(event, attempt))
                every { delivery.deliver(event) } returns NotificationDeliveryResult.RetryableFailure(" bad code! ")
                every { outbox.reschedule(event.id, now.plus(Duration.ofMinutes(minutes)), "BAD_CODE_") } returns Unit

                NotificationOutboxWorker(outbox, delivery, clock).dispatchBatch()

                verify(exactly = 1) { outbox.reschedule(event.id, now.plus(Duration.ofMinutes(minutes)), "BAD_CODE_") }
            }
        }

        it("네 번째 시도의 일시 실패는 추가 예약하지 않고 FAILED로 기록한다") {
            val outbox = mockk<NotificationOutboxWorkerJpaEntityService>()
            val delivery = mockk<NotificationDeliveryService>()
            every { outbox.claimBatch(now, Duration.ofMinutes(2), 100) } returns listOf(ClaimedNotification(event, 4))
            every { delivery.deliver(event) } returns NotificationDeliveryResult.RetryableFailure("provider_unavailable")
            every { outbox.markFailed(event.id, now, "PROVIDER_UNAVAILABLE") } returns Unit

            NotificationOutboxWorker(outbox, delivery, clock).dispatchBatch()

            verify(exactly = 1) { outbox.markFailed(event.id, now, "PROVIDER_UNAVAILABLE") }
            verify(exactly = 0) { outbox.reschedule(any(), any(), any()) }
        }

        it("영구 실패는 재시도하지 않고 안전한 오류 코드로 기록한다") {
            val outbox = mockk<NotificationOutboxWorkerJpaEntityService>()
            val delivery = mockk<NotificationDeliveryService>()
            every { outbox.claimBatch(now, Duration.ofMinutes(2), 100) } returns listOf(ClaimedNotification(event, 1))
            every { delivery.deliver(event) } returns NotificationDeliveryResult.PermanentFailure("payload rejected")
            every { outbox.markFailed(event.id, now, "PAYLOAD_REJECTED") } returns Unit

            NotificationOutboxWorker(outbox, delivery, clock).dispatchBatch()

            verify(exactly = 1) { outbox.markFailed(event.id, now, "PAYLOAD_REJECTED") }
            verify(exactly = 0) { outbox.reschedule(any(), any(), any()) }
        }

        it("전달 adapter 예외는 민감한 예외 메시지를 기록하지 않고 재시도한다") {
            val outbox = mockk<NotificationOutboxWorkerJpaEntityService>()
            val delivery = mockk<NotificationDeliveryService>()
            every { outbox.claimBatch(now, Duration.ofMinutes(2), 100) } returns listOf(ClaimedNotification(event, 1))
            every { delivery.deliver(event) } throws IllegalStateException("token=secret-value")
            every { outbox.reschedule(event.id, now.plus(Duration.ofMinutes(1)), "UNEXPECTED_PROVIDER_ERROR") } returns Unit

            NotificationOutboxWorker(outbox, delivery, clock).dispatchBatch()

            verify(exactly = 1) { outbox.reschedule(event.id, now.plus(Duration.ofMinutes(1)), "UNEXPECTED_PROVIDER_ERROR") }
        }
    }
})
