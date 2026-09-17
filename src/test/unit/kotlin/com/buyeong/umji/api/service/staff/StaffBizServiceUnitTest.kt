package com.buyeong.umji.api.service.staff

import com.buyeong.umji.api.persistence.jdbc.service.cdi.StaffJdbcEntityService
import com.buyeong.umji.api.persistence.jpa.service.cdi.StaffJpaEntityService
import com.buyeong.umji.api.service.biz.StaffBizService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class StaffBizServiceUnitTest : DescribeSpec({

    val staffJpaEntityService = mockk<StaffJpaEntityService>()
    val staffJdbcEntityService = mockk<StaffJdbcEntityService>()
    val service = StaffBizService(staffJpaEntityService, staffJdbcEntityService)

    describe("findAllStaffIdByBranchId") {

        it("should return list of staff IDs for given branch ID") {
            // given
            val branchId = 100
            val expectedStaffIds = listOf(1, 2, 3, 4, 5)

            every { staffJdbcEntityService.getAllStaffIdByBranchId(branchId) } returns expectedStaffIds

            // when
            val result = service.findAllStaffIdByBranchId(branchId)

            // then
            result shouldContainExactly expectedStaffIds
            verify(exactly = 1) { staffJdbcEntityService.getAllStaffIdByBranchId(branchId) }
        }

        it("should return empty list when no staff found for branch ID") {
            // given
            val branchId = 999

            every { staffJdbcEntityService.getAllStaffIdByBranchId(branchId) } returns emptyList()

            // when
            val result = service.findAllStaffIdByBranchId(branchId)

            // then
            result.shouldBeEmpty()
            verify(exactly = 1) { staffJdbcEntityService.getAllStaffIdByBranchId(branchId) }
        }

        it("should return single staff ID when only one staff in branch") {
            // given
            val branchId = 50
            val expectedStaffIds = listOf(42)

            every { staffJdbcEntityService.getAllStaffIdByBranchId(branchId) } returns expectedStaffIds

            // when
            val result = service.findAllStaffIdByBranchId(branchId)

            // then
            result.size shouldBe 1
            result.first() shouldBe 42
        }
    }
})