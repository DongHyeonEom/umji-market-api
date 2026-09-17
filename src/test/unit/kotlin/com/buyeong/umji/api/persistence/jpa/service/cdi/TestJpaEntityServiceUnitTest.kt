package com.buyeong.umji.api.persistence.jpa.service.cdi

import com.buyeong.umji.api.dto.TestDto
import com.buyeong.umji.api.mapper.TestMapper
import com.buyeong.umji.api.persistence.jpa.entity.cdi.TestJpaEntity
import com.buyeong.umji.api.persistence.jpa.repository.cdi.TestJpaRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.util.Optional

class TestJpaEntityServiceUnitTest : DescribeSpec({

    val testJpaRepository = mockk<TestJpaRepository>()
    val mapper = mockk<TestMapper>()
    val service = TestJpaEntityService(testJpaRepository, mapper)

    beforeTest {
        clearMocks(testJpaRepository, mapper)
    }

    describe("get") {

        it("should return TestDto when entity exists") {
            // given
            val id = 1
            val jsonData = listOf(mapOf("key1" to "value1"))
            val entity = mockk<TestJpaEntity>()
            val expectedDto = TestDto(id = id, json = jsonData)

            every { testJpaRepository.findById(id) } returns Optional.of(entity)
            every { mapper.toDto(entity) } returns expectedDto

            // when
            val result = service.get(id)

            // then
            result shouldBe expectedDto
            verify(exactly = 1) { testJpaRepository.findById(id) }
            verify(exactly = 1) { mapper.toDto(entity) }
        }

        it("should throw NoSuchElementException when entity does not exist") {
            // given
            val id = 999

            every { testJpaRepository.findById(id) } returns Optional.empty()

            // when & then
            shouldThrow<NoSuchElementException> {
                service.get(id)
            }
        }
    }

    describe("create") {

        it("should create and return TestDto when id is 0") {
            // given
            val jsonData = listOf(mapOf("key1" to "value1"))
            val inputDto = TestDto(id = 0, json = jsonData)
            val entity = mockk<TestJpaEntity>()
            val savedEntity = mockk<TestJpaEntity>()
            val expectedDto = TestDto(id = 1, json = jsonData)

            every { mapper.toEntity(inputDto) } returns entity
            every { testJpaRepository.save(entity) } returns savedEntity
            every { mapper.toDto(savedEntity) } returns expectedDto

            // when
            val result = service.create(inputDto)

            // then
            result shouldBe expectedDto
            verify(exactly = 1) { testJpaRepository.save(entity) }
        }

        it("should throw IllegalArgumentException when id is not 0") {
            // given
            val inputDto = TestDto(id = 123, json = listOf(mapOf("key" to "value")))

            // when & then
            val exception = shouldThrow<IllegalArgumentException> {
                service.create(inputDto)
            }
            exception.message shouldBe "ID must be zero for new staff creation"
        }

        it("should throw IllegalStateException when mapper returns null") {
            // given
            val inputDto = TestDto(id = 0, json = listOf(mapOf("key" to "value")))
            val entity = mockk<TestJpaEntity>()
            val savedEntity = mockk<TestJpaEntity>()

            every { mapper.toEntity(inputDto) } returns entity
            every { testJpaRepository.save(entity) } returns savedEntity
            every { mapper.toDto(savedEntity) } returns null

            // when & then
            val exception = shouldThrow<IllegalStateException> {
                service.create(inputDto)
            }
            exception.message shouldBe "Failed to convert entity to DTO"
        }
    }

    describe("modify") {

        it("should modify and return TestDto when entity exists") {
            // given
            val jsonData = listOf(mapOf("updated" to "value"))
            val inputDto = TestDto(id = 1, json = jsonData)
            val existingEntity = mockk<TestJpaEntity>()
            val updatedEntity = mockk<TestJpaEntity>()
            val entityToSave = mockk<TestJpaEntity>()

            every { testJpaRepository.findById(inputDto.id) } returns Optional.of(existingEntity)
            every { mapper.toEntity(inputDto) } returns entityToSave
            every { testJpaRepository.save(entityToSave) } returns updatedEntity
            every { mapper.toDto(updatedEntity) } returns inputDto

            // when
            val result = service.modify(inputDto)

            // then
            result shouldBe inputDto
            verify(exactly = 1) { testJpaRepository.findById(inputDto.id) }
            verify(exactly = 1) { testJpaRepository.save(entityToSave) }
        }

        it("should throw NoSuchElementException when entity does not exist") {
            // given
            val inputDto = TestDto(id = 999, json = listOf(mapOf("key" to "value")))

            every { testJpaRepository.findById(inputDto.id) } returns Optional.empty()

            // when & then
            val exception = shouldThrow<NoSuchElementException> {
                service.modify(inputDto)
            }
            exception.message shouldBe "Test with id ${inputDto.id} not found"
        }

        it("should throw IllegalStateException when mapper returns null") {
            // given
            val inputDto = TestDto(id = 1, json = listOf(mapOf("key" to "value")))
            val existingEntity = mockk<TestJpaEntity>()
            val updatedEntity = mockk<TestJpaEntity>()
            val entityToSave = mockk<TestJpaEntity>()

            every { testJpaRepository.findById(inputDto.id) } returns Optional.of(existingEntity)
            every { mapper.toEntity(inputDto) } returns entityToSave
            every { testJpaRepository.save(entityToSave) } returns updatedEntity
            every { mapper.toDto(updatedEntity) } returns null

            // when & then
            val exception = shouldThrow<IllegalStateException> {
                service.modify(inputDto)
            }
            exception.message shouldBe "Failed to convert entity to DTO"
        }
    }

    describe("delete") {

        it("should delete entity by id") {
            // given
            val id = 1

            every { testJpaRepository.deleteById(id) } just runs

            // when
            service.delete(id)

            // then
            verify(exactly = 1) { testJpaRepository.deleteById(id) }
        }
    }
})