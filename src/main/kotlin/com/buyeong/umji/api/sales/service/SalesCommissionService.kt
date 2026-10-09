package com.buyeong.umji.api.sales.service

import com.buyeong.umji.api.exception.InvalidRequestParameterException
import com.buyeong.umji.api.persistence.jpa.sales.service.SalesCommissionJpaEntityService
import com.buyeong.umji.api.sales.dto.SalesCommissionPageDto
import com.buyeong.umji.api.sales.dto.SalesCommissionSettlementResultDto
import com.buyeong.umji.api.sales.dto.SalesCommissionViewDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

@Service
class SalesCommissionService(
    private val commissions: SalesCommissionJpaEntityService,
) {
    @Transactional
    fun snapshotOrder(orderId: UUID, buyerAccountId: UUID, orderedAt: Instant, netItemSalesExTax: Long) {
        require(netItemSalesExTax >= 0) { "인센티브 기준액은 0 이상이어야 합니다." }
        val assignment = commissions.assignmentAt(buyerAccountId, orderedAt)
        val rate = assignment.commissionRateBps
        val commission = if (rate == null) 0 else calculate(netItemSalesExTax, rate)
        commissions.createSnapshot(
            orderId,
            assignment,
            netItemSalesExTax,
            commission,
            if (rate == null) NOT_APPLICABLE else WAITING,
        )
    }

    @Transactional(readOnly = true)
    fun mine(accountId: UUID, page: Int, size: Int): SalesCommissionPageDto = commissions.mine(accountId, page, size)

    @Transactional(readOnly = true)
    fun all(page: Int, size: Int): SalesCommissionPageDto = commissions.all(page, size)

    @Transactional
    fun settle(month: YearMonth, operatorId: UUID): SalesCommissionSettlementResultDto {
        if (!month.isBefore(YearMonth.now(KST))) {
            throw InvalidRequestParameterException("마감된 월만 정산할 수 있습니다.")
        }
        return commissions.settle(month, operatorId)
    }

    @Transactional
    fun markPaid(commissionId: UUID, operatorId: UUID): SalesCommissionViewDto = commissions.markPaid(commissionId, operatorId)

    @Transactional
    fun reverseOrder(orderId: UUID, reasonCode: String) = commissions.reverseOrder(orderId, reasonCode)

    private fun calculate(basis: Long, rateBps: Int): Long = BigDecimal.valueOf(basis)
        .multiply(BigDecimal.valueOf(rateBps.toLong()))
        .divide(BASIS_POINTS, 0, RoundingMode.HALF_UP)
        .longValueExact()

    private companion object {
        const val NOT_APPLICABLE = "NOT_APPLICABLE"
        const val WAITING = "WAITING"
        val BASIS_POINTS: BigDecimal = BigDecimal.valueOf(10_000)
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}