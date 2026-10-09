package com.buyeong.umji.api.persistence.jpa.sales.service

import com.buyeong.umji.api.exception.InvalidRequestParameterException
import com.buyeong.umji.api.exception.ItemNotFoundException
import com.buyeong.umji.api.domain.sales.dto.SalesAssignmentCommandDto
import com.buyeong.umji.api.domain.sales.dto.SalesAssignmentViewDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.util.UUID

@Service
@Transactional
class OrganizationSalesAssignmentJpaEntityService(
    private val jdbc: JdbcTemplate,
) {
    @Transactional(readOnly = true)
    fun history(organizationPublicId: UUID): List<SalesAssignmentViewDto> {
        val organizationId = organizationId(organizationPublicId) ?: throw ItemNotFoundException("구매 Organization을 찾을 수 없습니다.")
        return historyFor(organizationId)
    }

    fun assign(organizationPublicId: UUID, command: SalesAssignmentCommandDto): List<SalesAssignmentViewDto> {
        val organizationId = jdbc.query(
            """SELECT organization.id FROM organization
                JOIN organization_capability ON organization_capability.organization_id = organization.id
                    AND organization_capability.capability_code = 'BUYER'
                WHERE organization.public_id = ? AND organization.status = 'ACTIVE'
                FOR UPDATE
            """.trimIndent(),
            { result, _ -> result.getLong("id") },
            organizationPublicId.toBytes(),
        ).firstOrNull() ?: throw ItemNotFoundException("활성 구매 Organization을 찾을 수 없습니다.")

        val salesAccountId = jdbc.query(
            """SELECT account.id FROM account
                JOIN account_role ON account_role.account_id = account.id
                JOIN role ON role.id = account_role.role_id AND role.code = 'SALES_MANAGER'
                WHERE account.public_id = ? AND account.status = 'ACTIVE'
            """.trimIndent(),
            { result, _ -> result.getLong("id") },
            command.salesAccountId.toBytes(),
        ).firstOrNull() ?: throw InvalidRequestParameterException("활성 SALES_MANAGER 계정만 담당자로 배정할 수 있습니다.")

        val assignedByAccountId = jdbc.query(
            "SELECT id FROM account WHERE public_id = ? AND status = 'ACTIVE'",
            { result, _ -> result.getLong("id") },
            command.assignedByAccountId.toBytes(),
        ).firstOrNull() ?: throw ItemNotFoundException("배정 처리자 계정을 찾을 수 없습니다.")

        val current = jdbc.query(
            """SELECT public_id, sales_account_id, commission_rate_bps FROM organization_sales_assignment
                WHERE organization_id = ? AND valid_until IS NULL
                ORDER BY valid_from DESC LIMIT 1 FOR UPDATE
            """.trimIndent(),
            { result, _ ->
                CurrentAssignment(
                    result.getBytes("public_id").toUuid(),
                    result.getLong("sales_account_id"),
                    result.getObject("commission_rate_bps")?.let { (it as Number).toInt() },
                )
            },
            organizationId,
        ).firstOrNull()

        if (current?.salesAccountId == salesAccountId && current.commissionRateBps == command.commissionRateBps) {
            return historyFor(organizationId)
        }

        val effectiveAt = jdbc.queryForObject(
            """SELECT GREATEST(CURRENT_TIMESTAMP(3),
                TIMESTAMPADD(MICROSECOND, 1000, COALESCE(
                    (SELECT MAX(valid_from) FROM organization_sales_assignment WHERE organization_id = ?),
                    CURRENT_TIMESTAMP(3)
                )))
            """.trimIndent(),
            Timestamp::class.java,
            organizationId,
        )!!.toInstant()
        current?.let {
            jdbc.update(
                "UPDATE organization_sales_assignment SET valid_until = ? WHERE public_id = ?",
                Timestamp.from(effectiveAt),
                it.publicId.toBytes(),
            )
        }
        jdbc.update(
            """INSERT INTO organization_sales_assignment
                (public_id, organization_id, sales_account_id, commission_rate_bps, assignment_reason,
                 valid_from, valid_until, assigned_by_account_id)
                VALUES (?, ?, ?, ?, ?, ?, NULL, ?)
            """.trimIndent(),
            UUID.randomUUID().toBytes(),
            organizationId,
            salesAccountId,
            command.commissionRateBps,
            command.assignmentReason,
            Timestamp.from(effectiveAt),
            assignedByAccountId,
        )
        return historyFor(organizationId)
    }

    private fun organizationId(publicId: UUID): Long? = jdbc.query(
        """SELECT organization.id FROM organization
            JOIN organization_capability ON organization_capability.organization_id = organization.id
                AND organization_capability.capability_code = 'BUYER'
            WHERE organization.public_id = ? AND organization.status = 'ACTIVE'
        """.trimIndent(),
        { result, _ -> result.getLong("id") },
        publicId.toBytes(),
    ).firstOrNull()

    private fun historyFor(organizationId: Long): List<SalesAssignmentViewDto> = jdbc.query(
        """SELECT assignment.public_id, sales_account.public_id AS sales_account_public_id,
                  sales_account.name AS sales_account_name, assignment.commission_rate_bps,
                  assignment.assignment_reason, assignment.valid_from, assignment.valid_until,
                  assigned_by.public_id AS assigned_by_public_id
            FROM organization_sales_assignment assignment
            JOIN account sales_account ON sales_account.id = assignment.sales_account_id
            JOIN account assigned_by ON assigned_by.id = assignment.assigned_by_account_id
            WHERE assignment.organization_id = ?
            ORDER BY assignment.valid_from DESC, assignment.id DESC
        """.trimIndent(),
        { result, _ ->
            SalesAssignmentViewDto(
                result.getBytes("public_id").toUuid(),
                result.getBytes("sales_account_public_id").toUuid(),
                result.getString("sales_account_name"),
                result.getObject("commission_rate_bps")?.let { (it as Number).toInt() },
                result.getString("assignment_reason"),
                result.getTimestamp("valid_from").toInstant(),
                result.getTimestamp("valid_until")?.toInstant(),
                result.getBytes("assigned_by_public_id").toUuid(),
            )
        },
        organizationId,
    )

    private data class CurrentAssignment(
        val publicId: UUID,
        val salesAccountId: Long,
        val commissionRateBps: Int?,
    )

    private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16)
        .putLong(mostSignificantBits)
        .putLong(leastSignificantBits)
        .array()

    private fun ByteArray.toUuid(): UUID {
        val buffer = ByteBuffer.wrap(this)
        return UUID(buffer.long, buffer.long)
    }
}