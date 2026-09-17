package com.buyeong.umji.api.service.staff

import com.buyeong.umji.api.context.staff.StaffBuilder
import com.buyeong.umji.api.dto.StaffDto
import com.buyeong.umji.api.persistence.jdbc.service.cdi.StaffJdbcEntityService
import com.buyeong.umji.api.service.biz.StaffBizService
import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.mockk.every
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@Transactional
@SpringBootTest
class StaffBizServiceTest(
    private val service: StaffBizService,
) : BehaviorSpec() {
    @MockkBean
    lateinit var staffJdbcEntityService: StaffJdbcEntityService

    init {
        Context("Staff CRUD") {
            val staff = StaffBuilder.staffDtoBuilder().set(StaffDto::id, null).sample()

            Given("New Staff") {
                When("Staff Created") {
                    val createdStaff = service.add(staff)
                    Then("Staff should be created successfully with all fields") {
                        createdStaff.shouldBeEqualToIgnoringFields(
                            staff,
                            StaffDto::id,
                            StaffDto::createdAt,
                            StaffDto::createdBy,
                            StaffDto::updatedAt,
                            StaffDto::updatedBy,
                        )
                    }
                }
            }

            Given("Existing Staff") {
                val existingStaff = service.add(staff)

                When("Staff Updated") {
                    val updatedDto =
                        existingStaff.copy(
                            firstname = "Updated",
                            email = "updated@buyeong.com",
                            homePhone = "02-9999-9999",
                        )
                    val updatedStaff = service.modify(updatedDto)
                    Then("Staff should be updated successfully") {
                        updatedStaff.firstname shouldBe updatedDto.firstname
                        updatedStaff.email shouldBe updatedDto.email
                        updatedStaff.homePhone shouldBe updatedDto.homePhone
                    }
                }

                When("Staff Deleted") {
                    service.remove(existingStaff.id)
                    Then("Staff should not be found after deletion") {
                        shouldThrowAny {
                            service.find(existingStaff.id)
                        }
                    }
                }
            }
        }

        Context("Staff by Branch") {
            Given("Branch with active staff members") {
                val branchId = 100
                val expectedStaffIds = listOf(1, 2, 3, 4, 5)

                every { staffJdbcEntityService.getAllStaffIdByBranchId(branchId) } returns expectedStaffIds

                When("Finding all staff IDs by branch ID") {
                    val result = service.findAllStaffIdByBranchId(branchId)

                    Then("Should return all active staff IDs in the branch") {
                        result shouldContainExactly expectedStaffIds
                    }
                }
            }

            Given("Branch with no staff members") {
                val branchId = 999

                every { staffJdbcEntityService.getAllStaffIdByBranchId(branchId) } returns emptyList()

                When("Finding all staff IDs by branch ID") {
                    val result = service.findAllStaffIdByBranchId(branchId)

                    Then("Should return empty list") {
                        result.shouldBeEmpty()
                    }
                }
            }
        }
    }
}