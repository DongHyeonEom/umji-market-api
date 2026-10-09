package com.buyeong.umji.api.persistence.jpa.sales.service

import com.buyeong.umji.api.domain.payment.dto.PaymentRecordDto
import com.buyeong.umji.api.domain.sales.dto.SalesAssignmentSnapshotDto
import com.buyeong.umji.api.domain.sales.dto.SalesCommissionPageDto
import com.buyeong.umji.api.domain.sales.dto.SalesCommissionSettlementResultDto
import com.buyeong.umji.api.domain.sales.dto.SalesCommissionViewDto
import com.buyeong.umji.api.exception.InvalidRequestParameterException
import com.buyeong.umji.api.exception.ItemNotFoundException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.ByteBuffer
import java.sql.Timestamp
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

@Service
@Transactional
class SalesCommissionJpaEntityService(
    private val jdbc: JdbcTemplate,
) {
    fun assignmentAt(accountPublicId: UUID, orderedAt: Instant): SalesAssignmentSnapshotDto {
        val buyerOrganizationId = jdbc.query(
            """SELECT organization.id FROM account
                JOIN organization_member ON organization_member.account_id = account.id AND organization_member.status = 'ACTIVE'
                JOIN organization ON organization.id = organization_member.organization_id AND organization.status = 'ACTIVE'
                JOIN organization_capability ON organization_capability.organization_id = organization.id
                    AND organization_capability.capability_code = 'BUYER'
                WHERE account.public_id = ?
            """.trimIndent(),
            { result, _ -> result.getLong("id") },
            accountPublicId.toBytes(),
        ).firstOrNull() ?: throw ItemNotFoundException("주문 계정의 활성 구매 Organization을 찾을 수 없습니다.")

        val assignment = jdbc.query(
            """SELECT id, sales_account_id, commission_rate_bps FROM organization_sales_assignment
                WHERE organization_id = ? AND valid_from <= ? AND (valid_until IS NULL OR valid_until > ?)
                ORDER BY valid_from DESC LIMIT 1
            """.trimIndent(),
            { result, _ ->
                SalesAssignmentSnapshotDto(
                    buyerOrganizationId,
                    result.getLong("id"),
                    result.getLong("sales_account_id"),
                    result.getObject("commission_rate_bps")?.let { (it as Number).toInt() },
                )
            },
            buyerOrganizationId,
            Timestamp.from(orderedAt),
            Timestamp.from(orderedAt),
        ).firstOrNull()
        return assignment ?: SalesAssignmentSnapshotDto(buyerOrganizationId, null, null, null)
    }

    fun createSnapshot(
        orderPublicId: UUID,
        assignment: SalesAssignmentSnapshotDto,
        basisAmount: Long,
        commissionAmount: Long,
        status: String,
    ) {
        val orderId = internalId("SELECT id FROM purchase_order WHERE public_id = ?", orderPublicId)
            ?: throw ItemNotFoundException("인센티브 대상 주문을 찾을 수 없습니다.")
        jdbc.update(
            """INSERT INTO sales_commission
                (public_id, order_id, organization_id, assignment_id, sales_account_id, rate_bps_snapshot,
                 basis_snapshot, basis_amount, commission_amount, status)
                VALUES (?, ?, ?, ?, ?, ?, 'NET_ITEM_SALES_EX_TAX', ?, ?, ?)
            """.trimIndent(),
            UUID.randomUUID().toBytes(),
            orderId,
            assignment.organizationId,
            assignment.assignmentId,
            assignment.salesAccountId,
            assignment.commissionRateBps,
            basisAmount,
            commissionAmount,
            status,
        )
        val commissionInternalId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long::class.java)!!
        jdbc.update(
            """INSERT INTO sales_commission_event
                (commission_id, event_type, amount_delta, idempotency_key, reason_code)
                VALUES (?, 'SNAPSHOT', 0, ?, 'ORDER_CREATED')
            """.trimIndent(),
            commissionInternalId,
            "SNAPSHOT:$orderPublicId",
        )
    }

    @Transactional(readOnly = true)
    fun mine(accountPublicId: UUID, page: Int, size: Int): SalesCommissionPageDto {
        validatePage(page, size)
        val accountId = internalId("SELECT id FROM account WHERE public_id = ?", accountPublicId)
            ?: throw ItemNotFoundException("영업 계정을 찾을 수 없습니다.")
        return page("sales_account_id = ?", accountId, page, size)
    }

    @Transactional(readOnly = true)
    fun all(page: Int, size: Int): SalesCommissionPageDto {
        validatePage(page, size)
        return page(null, null, page, size)
    }

    fun settle(month: YearMonth, operatorPublicId: UUID): SalesCommissionSettlementResultDto {
        val operatorId = internalId("SELECT id FROM account WHERE public_id = ? AND status = 'ACTIVE'", operatorPublicId)
            ?: throw ItemNotFoundException("정산 처리자 계정을 찾을 수 없습니다.")
        val periodStart = month.atDay(1)
        val cutoff = Timestamp.from(month.plusMonths(1).atDay(1).atStartOfDay(KST).toInstant())
        val eligible = jdbc.query(
            """SELECT commission.id, commission.public_id, commission.commission_amount,
                      GREATEST(shipment.updated_at, payment_paid.paid_at) AS qualified_at
                FROM sales_commission commission
                JOIN purchase_order ON purchase_order.id = commission.order_id AND purchase_order.status = 'PAID'
                JOIN order_payment payment ON payment.order_id = purchase_order.id AND payment.status = 'PAYMENT_CONFIRMED'
                JOIN order_shipment shipment ON shipment.order_id = purchase_order.id AND shipment.status = 'DELIVERED'
                JOIN (
                    SELECT payment_id, MIN(changed_at) AS paid_at
                    FROM order_payment_status_history
                    WHERE to_status = 'PAYMENT_CONFIRMED'
                    GROUP BY payment_id
                ) payment_paid ON payment_paid.payment_id = payment.id
                WHERE commission.status = 'WAITING'
                  AND shipment.updated_at < ?
                  AND payment_paid.paid_at < ?
            """.trimIndent(),
            { result, _ ->
                EligibleCommission(
                    result.getLong("id"),
                    result.getBytes("public_id").toUuid(),
                    result.getLong("commission_amount"),
                    result.getTimestamp("qualified_at").toInstant(),
                )
            },
            cutoff,
            cutoff,
        ).filter { YearMonth.from(it.qualifiedAt.atZone(KST)) == month }

        var count = 0
        var total = 0L
        eligible.forEach { item ->
            val changed = jdbc.update(
                """UPDATE sales_commission SET status = 'PAYABLE', settlement_month = ?, qualified_at = ?
                    WHERE id = ? AND status = 'WAITING'
                """.trimIndent(),
                java.sql.Date.valueOf(periodStart),
                Timestamp.from(item.qualifiedAt),
                item.internalId,
            )
            if (changed == 1) {
                jdbc.update(
                    """INSERT INTO sales_commission_event
                        (commission_id, event_type, amount_delta, idempotency_key, processed_by_account_id, reason_code)
                        VALUES (?, 'ACCRUED', ?, ?, ?, 'MONTH_END_SETTLEMENT')
                    """.trimIndent(),
                    item.internalId,
                    item.amount,
                    "ACCRUED:${item.publicId}:$month",
                    operatorId,
                )
                count++
                total = Math.addExact(total, item.amount)
            }
        }
        return SalesCommissionSettlementResultDto(month, count, total)
    }

    fun markPaid(commissionPublicId: UUID, operatorPublicId: UUID): SalesCommissionViewDto {
        val operatorId = internalId("SELECT id FROM account WHERE public_id = ? AND status = 'ACTIVE'", operatorPublicId)
            ?: throw ItemNotFoundException("지급 처리자 계정을 찾을 수 없습니다.")
        val record = jdbc.query(
            "SELECT id, commission_amount, status FROM sales_commission WHERE public_id = ? FOR UPDATE",
            { result, _ -> PaymentRecordDto(result.getLong("id"), result.getLong("commission_amount"), result.getString("status")) },
            commissionPublicId.toBytes(),
        ).firstOrNull() ?: throw ItemNotFoundException("인센티브 원장을 찾을 수 없습니다.")
        if (record.status == PAID) return findOne(commissionPublicId)
        if (record.status != PAYABLE) throw InvalidRequestParameterException("PAYABLE 상태의 인센티브만 지급 처리할 수 있습니다.")

        jdbc.update("UPDATE sales_commission SET status = 'PAID', paid_at = CURRENT_TIMESTAMP(3) WHERE id = ? AND status = 'PAYABLE'", record.internalId)
        jdbc.update(
            """INSERT INTO sales_commission_event
                (commission_id, event_type, amount_delta, idempotency_key, processed_by_account_id, reason_code)
                VALUES (?, 'PAID', ?, ?, ?, 'ADMIN_PAYOUT')
            """.trimIndent(),
            record.internalId,
            -record.amount,
            "PAID:$commissionPublicId",
            operatorId,
        )
        return findOne(commissionPublicId)
    }

    fun reverseOrder(orderPublicId: UUID, reasonCode: String) {
        val commission = jdbc.query(
            """SELECT commission.id, commission.public_id, commission.commission_amount, commission.status
                FROM sales_commission commission
                JOIN purchase_order ON purchase_order.id = commission.order_id
                WHERE purchase_order.public_id = ? FOR UPDATE
            """.trimIndent(),
            { result, _ ->
                PaymentRecordDto(
                    result.getLong("id"),
                    result.getLong("commission_amount"),
                    result.getString("status"),
                    result.getBytes("public_id").toUuid(),
                )
            },
            orderPublicId.toBytes(),
        ).firstOrNull() ?: return
        if (commission.status == NOT_APPLICABLE || commission.status == REVERSED) return

        val reversalAmount = if (commission.status == WAITING) 0L else -commission.amount
        jdbc.update("UPDATE sales_commission SET status = 'REVERSED' WHERE id = ? AND status <> 'REVERSED'", commission.internalId)
        jdbc.update(
            """INSERT INTO sales_commission_event
                (commission_id, event_type, amount_delta, idempotency_key, reason_code)
                VALUES (?, 'REVERSED', ?, ?, ?)
            """.trimIndent(),
            commission.internalId,
            reversalAmount,
            "REVERSED:$orderPublicId",
            reasonCode,
        )
    }

    private fun findOne(publicId: UUID): SalesCommissionViewDto = jdbc.query(
        commissionSelect("commission.public_id = ?"),
        { result, _ -> result.toView() },
        publicId.toBytes(),
    ).firstOrNull() ?: throw ItemNotFoundException("인센티브 원장을 찾을 수 없습니다.")

    private fun page(where: String?, accountId: Long?, page: Int, size: Int): SalesCommissionPageDto {
        val predicate = where?.let { "WHERE $it" }.orEmpty()
        val total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM sales_commission commission $predicate",
            Long::class.java,
            *if (accountId == null) emptyArray() else arrayOf(accountId),
        ) ?: 0L
        val items = jdbc.query(
            "${commissionSelect(predicate.removePrefix("WHERE "))} ORDER BY commission.created_at DESC, commission.id DESC LIMIT ? OFFSET ?",
            { result, _ -> result.toView() },
            *buildList<Any> {
                if (accountId != null) add(accountId)
                add(size)
                add(page.toLong() * size)
            }.toTypedArray(),
        )
        return SalesCommissionPageDto(items, page, size, total, if (total == 0L) 0 else ((total + size - 1) / size).toInt())
    }

    private fun commissionSelect(predicate: String) =
        """SELECT commission.public_id, purchase_order.public_id AS order_public_id,
                  sales_account.public_id AS sales_account_public_id, commission.rate_bps_snapshot,
                  commission.basis_amount, commission.commission_amount, commission.status,
                  commission.settlement_month, commission.qualified_at, commission.paid_at, commission.created_at
            FROM sales_commission commission
            JOIN purchase_order ON purchase_order.id = commission.order_id
            LEFT JOIN account sales_account ON sales_account.id = commission.sales_account_id
            ${if (predicate.isBlank()) "" else "WHERE $predicate"}
        """.trimIndent()

    private fun java.sql.ResultSet.toView() = SalesCommissionViewDto(
        getBytes("public_id").toUuid(),
        getBytes("order_public_id").toUuid(),
        getBytes("sales_account_public_id")?.toUuid(),
        getObject("rate_bps_snapshot")?.let { (it as Number).toInt() },
        getLong("basis_amount"),
        getLong("commission_amount"),
        getString("status"),
        getDate("settlement_month")?.toLocalDate(),
        getTimestamp("qualified_at")?.toInstant(),
        getTimestamp("paid_at")?.toInstant(),
        getTimestamp("created_at").toInstant(),
    )

    private fun internalId(sql: String, publicId: UUID): Long? = jdbc.query(sql, { result, _ -> result.getLong("id") }, publicId.toBytes()).firstOrNull()

    private fun validatePage(page: Int, size: Int) {
        require(page >= 0 && size in 1..100) { "페이지 값이 올바르지 않습니다." }
    }

    private data class EligibleCommission(val internalId: Long, val publicId: UUID, val amount: Long, val qualifiedAt: Instant)
    private data class PaymentRecordDto(val internalId: Long, val amount: Long, val status: String, val publicId: UUID? = null)

    private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16).putLong(mostSignificantBits).putLong(leastSignificantBits).array()

    private fun ByteArray.toUuid(): UUID {
        val buffer = ByteBuffer.wrap(this)
        return UUID(buffer.long, buffer.long)
    }

    private companion object {
        const val NOT_APPLICABLE = "NOT_APPLICABLE"
        const val WAITING = "WAITING"
        const val PAYABLE = "PAYABLE"
        const val PAID = "PAID"
        const val REVERSED = "REVERSED"
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}