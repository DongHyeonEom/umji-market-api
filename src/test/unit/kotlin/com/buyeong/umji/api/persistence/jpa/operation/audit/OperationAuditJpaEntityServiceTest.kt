package com.buyeong.umji.api.persistence.jpa.operation.audit.service

import com.buyeong.umji.api.domain.operation.audit.dto.OperationAuditEventDto
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.jdbc.core.JdbcTemplate
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class OperationAuditJpaEntityServiceTest : DescribeSpec({
    val jdbc = mockk<JdbcTemplate>(relaxed = true)
    val adapter = OperationAuditJpaEntityService(jdbc)
    val actorId = UUID.randomUUID()
    val resourceId = UUID.randomUUID()
    val occurredAt = Instant.parse("2026-09-28T02:15:00Z")

    beforeTest {
        clearMocks(jdbc)
        every { jdbc.update(any<String>(), *anyVararg()) } returns 1
    }

    it("허용된 감사 필드만 저장한다") {
        val event = OperationAuditEventDto(
            actorId,
            "PATCH /api/operation/inventory/skus/{skuId}",
            "INVENTORY",
            resourceId,
            "trace-123",
            occurredAt,
        )

        adapter.record(event)

        verify(exactly = 1) {
            jdbc.update(
                match { it.startsWith("INSERT INTO operation_audit_log") },
                match<ByteArray> { it.contentEquals(actorId.toBytes()) },
                event.action,
                event.resourceType,
                match<ByteArray> { it.contentEquals(resourceId.toBytes()) },
                event.requestTraceId,
                Timestamp.from(occurredAt),
            )
        }
    }

    it("보존 만료 삭제를 제한된 batch로 실행한다") {
        val cutoff = Instant.parse("2024-09-29T00:00:00Z")

        adapter.purgeExpired(cutoff, 10_000)

        verify(exactly = 1) {
            jdbc.update(
                "DELETE FROM operation_audit_log WHERE occurred_at < ? ORDER BY occurred_at, id LIMIT ?",
                Timestamp.from(cutoff),
                10_000,
            )
        }
    }
}) {
    companion object {
        private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16).putLong(mostSignificantBits).putLong(leastSignificantBits).array()
    }
}