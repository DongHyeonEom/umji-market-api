package com.buyeong.umji.api.sales.service

import com.buyeong.umji.api.exception.InvalidRequestParameterException
import com.buyeong.umji.api.persistence.jpa.sales.service.SalesAssignmentSnapshot
import com.buyeong.umji.api.persistence.jpa.sales.service.SalesCommissionJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import java.time.YearMonth
import java.util.UUID

class SalesCommissionServiceTest : DescribeSpec({
    val persistence = mockk<SalesCommissionJpaEntityService>(relaxed = true)
    val service = SalesCommissionService(persistence)
    val orderId = UUID.randomUUID()
    val accountId = UUID.randomUUID()
    val orderedAt = Instant.parse("2026-08-15T00:00:00Z")

    describe("order commission snapshot") {
        it("calculates the snapshot with integer HALF_UP rounding") {
            val assignment = SalesAssignmentSnapshot(organizationId = 1, assignmentId = 2, salesAccountId = 3, commissionRateBps = 5_000)
            every { persistence.assignmentAt(accountId, orderedAt) } returns assignment

            service.snapshotOrder(orderId, accountId, orderedAt, 1)

            verify {
                persistence.createSnapshot(orderId, assignment, 1, 1, "WAITING")
            }
        }

        it("stores an order without a selected rate as not applicable") {
            val assignment = SalesAssignmentSnapshot(organizationId = 1, assignmentId = 2, salesAccountId = 3, commissionRateBps = null)
            every { persistence.assignmentAt(accountId, orderedAt) } returns assignment

            service.snapshotOrder(orderId, accountId, orderedAt, 100_000)

            verify {
                persistence.createSnapshot(orderId, assignment, 100_000, 0, "NOT_APPLICABLE")
            }
        }

        it("rejects a negative sale basis") {
            shouldThrow<IllegalArgumentException> {
                service.snapshotOrder(orderId, accountId, orderedAt, -1)
            }
        }
    }

    it("does not settle the current or a future month") {
        val currentMonth = YearMonth.now(java.time.ZoneId.of("Asia/Seoul"))
        shouldThrow<InvalidRequestParameterException> { service.settle(currentMonth, accountId) }
        shouldThrow<InvalidRequestParameterException> { service.settle(currentMonth.plusMonths(1), accountId) }
    }
})