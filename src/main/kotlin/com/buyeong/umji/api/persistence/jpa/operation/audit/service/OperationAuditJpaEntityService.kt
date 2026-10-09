package com.buyeong.umji.api.persistence.jpa.operation.audit.service

import com.buyeong.umji.api.domain.operation.audit.dto.OperationAuditEntryDto
import com.buyeong.umji.api.domain.operation.audit.dto.OperationAuditEventDto
import com.buyeong.umji.api.domain.operation.audit.dto.OperationAuditPageDto
import com.buyeong.umji.api.domain.operation.audit.dto.OperationAuditQueryDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class OperationAuditJpaEntityService(private val jdbc: JdbcTemplate) {
    fun record(event: OperationAuditEventDto) {
        jdbc.update(
            """INSERT INTO operation_audit_log
                (actor_public_id, action, resource_type, resource_public_id, request_trace_id, occurred_at)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            event.actorId?.toBytes(),
            event.action,
            event.resourceType,
            event.resourceId?.toBytes(),
            event.requestTraceId,
            Timestamp.from(event.occurredAt),
        )
    }

    @Transactional(readOnly = true)
    fun search(query: OperationAuditQueryDto): OperationAuditPageDto {
        val filters = mutableListOf<String>()
        val arguments = mutableListOf<Any>()
        query.actorId?.let {
            filters += "actor_public_id = ?"
            arguments += it.toBytes()
        }
        query.resourceType?.let {
            filters += "resource_type = ?"
            arguments += it
        }
        query.from?.let {
            filters += "occurred_at >= ?"
            arguments += Timestamp.from(it)
        }
        query.until?.let {
            filters += "occurred_at < ?"
            arguments += Timestamp.from(it)
        }
        val where = filters.takeIf { it.isNotEmpty() }?.joinToString(" AND ", prefix = " WHERE ").orEmpty()
        val total = jdbc.queryForObject("SELECT COUNT(*) FROM operation_audit_log$where", Long::class.java, *arguments.toTypedArray()) ?: 0L
        val items = jdbc.query(
            """SELECT id, actor_public_id, action, resource_type, resource_public_id, request_trace_id, occurred_at
                FROM operation_audit_log$where
                ORDER BY occurred_at DESC, id DESC
                LIMIT ? OFFSET ?
            """.trimIndent(),
            { result, _ ->
                OperationAuditEntryDto(
                    result.getLong("id"),
                    result.getBytes("actor_public_id")?.toUuid(),
                    result.getString("action"),
                    result.getString("resource_type"),
                    result.getBytes("resource_public_id")?.toUuid(),
                    result.getString("request_trace_id"),
                    result.getTimestamp("occurred_at").toInstant(),
                )
            },
            *arguments.plus(query.size).plus(query.page.toLong() * query.size).toTypedArray(),
        )
        val totalPages = if (total == 0L) 0 else ((total + query.size - 1) / query.size).toInt()
        return OperationAuditPageDto(items, query.page, query.size, total, totalPages)
    }

    fun purgeExpired(before: Instant, batchSize: Int): Int = jdbc.update(
        "DELETE FROM operation_audit_log WHERE occurred_at < ? ORDER BY occurred_at, id LIMIT ?",
        Timestamp.from(before),
        batchSize,
    )

    private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16).putLong(mostSignificantBits).putLong(leastSignificantBits).array()
    private fun ByteArray.toUuid(): UUID = ByteBuffer.wrap(this).let { UUID(it.long, it.long) }
}