package com.buyeong.umji.api.operation.audit.adapter

import com.buyeong.umji.api.operation.audit.application.port.`in`.OperationAuditUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class OperationAuditRetentionJob(private val audit: OperationAuditUseCase) {
    @Scheduled(cron = "0 15 3 * * *", zone = "UTC")
    fun purgeExpired() {
        val cutoff = Instant.now().minus(RETENTION)
        while (audit.purgeExpired(cutoff, DELETE_BATCH_SIZE) == DELETE_BATCH_SIZE) Unit
    }

    private companion object {
        val RETENTION = Duration.ofDays(730)
        const val DELETE_BATCH_SIZE = 10_000
    }
}