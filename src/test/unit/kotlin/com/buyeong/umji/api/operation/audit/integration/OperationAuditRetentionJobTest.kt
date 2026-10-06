package com.buyeong.umji.api.operation.audit.integration

import com.buyeong.umji.api.operation.audit.service.OperationAuditService
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class OperationAuditRetentionJobTest : DescribeSpec({
    val audit = mockk<OperationAuditService>(relaxed = true)
    val job = OperationAuditRetentionJob(audit)
    val deleteCalls = AtomicInteger()

    beforeTest {
        clearMocks(audit)
        deleteCalls.set(0)
        every { audit.purgeExpired(any<Instant>(), 10_000) } answers {
            if (deleteCalls.incrementAndGet() == 1) 10_000 else 3
        }
    }

    it("만료 행을 10,000건 단위로 삭제하고 마지막 일부 batch 후 종료한다") {
        job.purgeExpired()

        verify(exactly = 2) { audit.purgeExpired(any<Instant>(), 10_000) }
    }
})
