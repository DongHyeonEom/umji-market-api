package com.buyeong.umji.api.notification.integration

import com.buyeong.umji.api.notification.service.NotificationOutboxWorker
import org.springframework.scheduling.annotation.Scheduled

class NotificationOutboxDispatchJob(private val worker: NotificationOutboxWorker) {
    @Scheduled(fixedDelayString = "\${umji.notification.worker.fixed-delay-ms:5000}")
    fun dispatch() {
        worker.dispatchBatch()
    }
}