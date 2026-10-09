package com.buyeong.umji.api.sales.service

import com.buyeong.umji.api.exception.InvalidRequestParameterException
import com.buyeong.umji.api.persistence.jpa.sales.service.OrganizationSalesAssignmentJpaEntityService
import com.buyeong.umji.api.sales.model.SalesAssignmentCommand
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.mockk.every
import io.mockk.mockk
import java.util.UUID

class SalesAssignmentServiceTest : DescribeSpec({
    val assignments = mockk<OrganizationSalesAssignmentJpaEntityService>()
    val service = SalesAssignmentService(assignments)
    val organizationId = UUID.randomUUID()
    val salesAccountId = UUID.randomUUID()
    val operatorId = UUID.randomUUID()

    describe("sales assignment validation") {
        it("allows an unset rate and delegates a valid assignment") {
            every { assignments.assign(organizationId, any()) } returns emptyList()

            service.assign(
                organizationId,
                SalesAssignmentCommand(salesAccountId, null, "INITIAL_ASSIGNMENT", operatorId),
            ).shouldBeEmpty()
        }

        it("rejects rates outside the configured basis point range") {
            shouldThrow<InvalidRequestParameterException> {
                service.assign(
                    organizationId,
                    SalesAssignmentCommand(salesAccountId, 10_001, "RATE_CHANGE", operatorId),
                )
            }
        }

        it("rejects blank or overlong assignment reasons") {
            shouldThrow<InvalidRequestParameterException> {
                service.assign(
                    organizationId,
                    SalesAssignmentCommand(salesAccountId, 30, " ", operatorId),
                )
            }
            shouldThrow<InvalidRequestParameterException> {
                service.assign(
                    organizationId,
                    SalesAssignmentCommand(salesAccountId, 30, "A".repeat(31), operatorId),
                )
            }
        }
    }
})
