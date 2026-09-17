package com.buyeong.umji.api.persistence.jpa.service.cdi

import com.buyeong.umji.api.context.staff.StaffBuilder
import com.buyeong.umji.api.dto.StaffDto
import com.buyeong.umji.api.mapper.StaffMapper
import com.buyeong.umji.api.persistence.jpa.entity.cdi.StaffJpaEntity
import com.buyeong.umji.api.persistence.jpa.repository.cdi.StaffJpaRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.util.Optional

class StaffJpaEntityServiceUnitTest : DescribeSpec({

    val staffJpaRepository = mockk<StaffJpaRepository>()
    val mapper = mockk<StaffMapper>()
    val service = StaffJpaEntityService(staffJpaRepository, mapper)

    beforeTest {
        clearMocks(staffJpaRepository, mapper)
    }

    describe("get") {

        it("should return StaffDto when entity exists") {
            // given
            val id = 1
            val entity = mockk<StaffJpaEntity>()
            val expectedDto = StaffBuilder.staffDtoBuilder().set(StaffDto::id, id).sample()

            every { staffJpaRepository.findById(id) } returns Optional.of(entity)
            every { mapper.toDto(entity) } returns expectedDto

            // when
            val result = service.get(id)

            // then
            result shouldBe expectedDto
            verify(exactly = 1) { staffJpaRepository.findById(id) }
            verify(exactly = 1) { mapper.toDto(entity) }
        }

        it("should return null when entity does not exist") {
            // given
            val id = 999

            every { staffJpaRepository.findById(id) } returns Optional.empty()
            every { mapper.toDto(null as StaffJpaEntity?) } returns null

            // when
            val result = service.get(id)

            // then
            result.shouldBeNull()
        }
    }

    describe("getAllByBranchId") {

        it("should return list of StaffDto for given branch ID") {
            // given
            val branchId = 100
            val entities = listOf(mockk<StaffJpaEntity>(), mockk<StaffJpaEntity>())
            val expectedDtos = listOf(
                StaffBuilder.staffDtoBuilder().set(StaffDto::branchId, branchId).sample(),
                StaffBuilder.staffDtoBuilder().set(StaffDto::branchId, branchId).sample(),
            )

            every { staffJpaRepository.getAllByBranchId(branchId) } returns entities
            every { mapper.toDtoList(entities) } returns expectedDtos

            // when
            val result = service.getAllByBranchId(branchId)

            // then
            result shouldHaveSize 2
            verify(exactly = 1) { staffJpaRepository.getAllByBranchId(branchId) }
            verify(exactly = 1) { mapper.toDtoList(entities) }
        }

        it("should return empty list when no staff found for branch ID") {
            // given
            val branchId = 999

            every { staffJpaRepository.getAllByBranchId(branchId) } returns emptyList()
            every { mapper.toDtoList(emptyList()) } returns emptyList()

            // when
            val result = service.getAllByBranchId(branchId)

            // then
            result.shouldBeEmpty()
        }
    }

    describe("create") {

        it("should create and return StaffDto when id is 0") {
            // given
            val inputDto = StaffBuilder.staffDtoBuilder().set(StaffDto::id, 0).sample()
            val entity = mockk<StaffJpaEntity>()
            val savedEntity = mockk<StaffJpaEntity>()
            val expectedDto = inputDto.copy(id = 1)

            every { mapper.toEntity(inputDto) } returns entity
            every { staffJpaRepository.save(entity) } returns savedEntity
            every { mapper.toDto(savedEntity) } returns expectedDto

            // when
            val result = service.create(inputDto)

            // then
            result shouldBe expectedDto
            verify(exactly = 1) { staffJpaRepository.save(entity) }
        }

        it("should throw IllegalArgumentException when id is not 0") {
            // given
            val inputDto = StaffBuilder.staffDtoBuilder().set(StaffDto::id, 123).sample()

            // when & then
            val exception = shouldThrow<IllegalArgumentException> {
                service.create(inputDto)
            }
            exception.message shouldBe "Staff ID must be zero for new staff creation"
        }
    }

    describe("update") {

        it("should update and return StaffDto when entity exists") {
            // given
            val inputDto = StaffBuilder.staffDtoBuilder().set(StaffDto::id, 1).sample()
            val existingEntity = mockk<StaffJpaEntity>()
            val updatedEntity = mockk<StaffJpaEntity>()
            val entityToSave = mockk<StaffJpaEntity>()

            every { staffJpaRepository.findById(inputDto.id) } returns Optional.of(existingEntity)
            every { mapper.toEntity(inputDto) } returns entityToSave
            every { staffJpaRepository.save(entityToSave) } returns updatedEntity
            every { mapper.toDto(updatedEntity) } returns inputDto

            // when
            val result = service.update(inputDto)

            // then
            result shouldBe inputDto
            verify(exactly = 1) { staffJpaRepository.findById(inputDto.id) }
            verify(exactly = 1) { staffJpaRepository.save(entityToSave) }
        }

        it("should throw NoSuchElementException when entity does not exist") {
            // given
            val inputDto = StaffBuilder.staffDtoBuilder().set(StaffDto::id, 999).sample()

            every { staffJpaRepository.findById(inputDto.id) } returns Optional.empty()

            // when & then
            val exception = shouldThrow<NoSuchElementException> {
                service.update(inputDto)
            }
            exception.message shouldBe "Staff with id ${inputDto.id} not found"
        }
    }

    describe("delete") {

        it("should delete entity by id") {
            // given
            val staffId = 1

            every { staffJpaRepository.deleteById(staffId) } just runs

            // when
            service.delete(staffId)

            // then
            verify(exactly = 1) { staffJpaRepository.deleteById(staffId) }
        }
    }
})