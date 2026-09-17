package com.buyeong.umji.api.persistence.jpa.service.cdi

import com.buyeong.umji.api.dto.StaffDto
import com.buyeong.umji.api.enums.StaffStatusEnum
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

/**
 * StaffJpaEntityService 통합 테스트.
 *
 * Testcontainers를 통해 실제 데이터베이스와 연동하여 테스트합니다.
 * - 실제 JPA Repository 사용
 * - 실제 MapStruct Mapper 사용
 * - 트랜잭션 롤백으로 테스트 격리
 */
@SpringBootTest
@Transactional
class StaffJpaEntityServiceIntegrationTest(
    private val service: StaffJpaEntityService,
) : BehaviorSpec({

    Given("Staff 생성을 위한 StaffDto") {
        val staffDto = createStaffDto(id = 0, branchId = 100)

        When("create 메서드를 호출하면") {
            val createdStaff = service.create(staffDto)

            Then("ID가 생성된 StaffDto가 반환된다") {
                createdStaff.shouldNotBeNull()
                createdStaff.id shouldBeGreaterThan 0
                createdStaff.lastname shouldBe staffDto.lastname
                createdStaff.status shouldBe staffDto.status
                createdStaff.branchId shouldBe staffDto.branchId
            }
        }
    }

    Given("ID가 0이 아닌 StaffDto") {
        val staffDto = createStaffDto(id = 123, branchId = 100)

        When("create 메서드를 호출하면") {
            Then("IllegalArgumentException이 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    service.create(staffDto)
                }
                exception.message shouldBe "Staff ID must be zero for new staff creation"
            }
        }
    }

    Given("저장된 Staff") {
        val staffDto = createStaffDto(id = 0, branchId = 200)
        val savedStaff = service.create(staffDto)

        When("get 메서드로 ID로 조회하면") {
            val foundStaff = service.get(savedStaff.id)

            Then("저장된 Staff가 조회된다") {
                foundStaff.shouldNotBeNull()
                foundStaff.id shouldBe savedStaff.id
                foundStaff.lastname shouldBe savedStaff.lastname
            }
        }

        When("존재하지 않는 ID로 조회하면") {
            val foundStaff = service.get(999999)

            Then("null이 반환된다") {
                foundStaff.shouldBeNull()
            }
        }
    }

    Given("동일 branchId를 가진 여러 Staff") {
        // 기존 데이터와 충돌하지 않도록 매우 큰 branchId 사용
        val uniqueBranchId = 9999000 + (System.currentTimeMillis() % 1000).toInt()
        val staff1 = service.create(createStaffDto(id = 0, branchId = uniqueBranchId, lastname = "Kim"))
        val staff2 = service.create(createStaffDto(id = 0, branchId = uniqueBranchId, lastname = "Lee"))
        service.create(createStaffDto(id = 0, branchId = uniqueBranchId + 1, lastname = "Park")) // 다른 branchId

        When("getAllByBranchId로 조회하면") {
            val staffList = service.getAllByBranchId(uniqueBranchId)

            Then("해당 branchId의 Staff만 조회된다") {
                staffList.size shouldBeGreaterThan 1
                staffList.all { it.branchId == uniqueBranchId } shouldBe true
                staffList.map { it.id } shouldContainAll listOf(staff1.id, staff2.id)
            }
        }

        When("Staff가 없는 branchId로 조회하면") {
            val nonExistentBranchId = 8888888
            val staffList = service.getAllByBranchId(nonExistentBranchId)

            Then("빈 리스트가 반환된다") {
                staffList shouldHaveSize 0
            }
        }
    }

    Given("수정할 Staff") {
        val staffDto = createStaffDto(id = 0, branchId = 500)
        val savedStaff = service.create(staffDto)

        When("update 메서드로 수정하면") {
            val updatedDto = savedStaff.copy(
                lastname = "UpdatedName",
                status = StaffStatusEnum.INACTIVE,
            )
            val result = service.update(updatedDto)

            Then("수정된 Staff가 반환된다") {
                result.shouldNotBeNull()
                result.id shouldBe savedStaff.id
                result.lastname shouldBe "UpdatedName"
                result.status shouldBe StaffStatusEnum.INACTIVE
            }

            And("조회 시 수정된 값이 확인된다") {
                val foundStaff = service.get(savedStaff.id)
                foundStaff.shouldNotBeNull()
                foundStaff.lastname shouldBe "UpdatedName"
                foundStaff.status shouldBe StaffStatusEnum.INACTIVE
            }
        }
    }

    Given("존재하지 않는 Staff ID로 update 시도") {
        val nonExistentDto = createStaffDto(id = 999999, branchId = 100)

        When("update 메서드를 호출하면") {
            Then("NoSuchElementException이 발생한다") {
                val exception = shouldThrow<NoSuchElementException> {
                    service.update(nonExistentDto)
                }
                exception.message shouldBe "Staff with id ${nonExistentDto.id} not found"
            }
        }
    }

    Given("삭제할 Staff") {
        val staffDto = createStaffDto(id = 0, branchId = 600)
        val savedStaff = service.create(staffDto)

        When("delete 메서드로 삭제하면") {
            service.delete(savedStaff.id)

            Then("조회 시 null이 반환된다") {
                val foundStaff = service.get(savedStaff.id)
                foundStaff.shouldBeNull()
            }
        }
    }
})

/**
 * 테스트용 StaffDto 생성 헬퍼 함수.
 */
private fun createStaffDto(
    id: Int,
    branchId: Int,
    lastname: String = "TestLastname",
    status: StaffStatusEnum = StaffStatusEnum.ACTIVE,
): StaffDto =
    StaffDto(
        id = id,
        status = status,
        recruitStatus = null,
        lastname = lastname,
        firstname = "TestFirstname",
        middlename = null,
        nickname = "TestNickname",
        nationalId = null,
        image = null,
        open = false,
        openOrder = null,
        openGroup = null,
        email = "test@example.com",
        homePhone = null,
        handPhone = "010-1234-5678",
        birthdate = null,
        gender = "M",
        nationality = null,
        bankAccount = null,
        bank = null,
        loginId = null,
        password = null,
        married = null,
        birthPlace = null,
        homepage = null,
        inKorea = null,
        educationLevel = null,
        note = null,
        teacher = null,
        onlineTeacherOnly = null,
        wmSupervisor = null,
        onlineTimeType = null,
        ibtTimeType = null,
        rcContactPoint = null,
        createdBy = 1,
        createdAt = null,
        updatedBy = 1,
        updatedAt = null,
        branchId = branchId,
        orgAppId = null,
        newLogin = null,
        loginBackup = null,
        tempType = null,
        eipEmployeeId = null,
        noneCdi = null,
        noneCdiCode = null,
        supervisor = null,
        manager = null,
        unrestAccess = null,
        passwordEncryption = null,
        passwordChangeDate = null,
        freelancer = null,
        admin = null,
        webLoginId = null,
        studentId = null,
        callingNumber = null,
        allimPushId = null,
        samsLoginId = null,
        workType = null,
    )