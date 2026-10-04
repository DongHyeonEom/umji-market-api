package com.buyeong.umji.api.notification.adapter

import com.buyeong.umji.api.notification.application.NotificationOutboxWorker
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnBean(NotificationOutboxWorker::class)
class NotificationOutboxDispatchJob(private val worker: NotificationOutboxWorker) {
    @Scheduled(fixedDelayString = "\${umji.notification.worker.fixed-delay-ms:5000}")
    fun dispatch() {
        worker.dispatchBatch()
    }
}