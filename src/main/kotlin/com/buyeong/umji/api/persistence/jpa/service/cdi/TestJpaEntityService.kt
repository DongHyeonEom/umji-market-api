package com.buyeong.umji.api.persistence.jpa.service.cdi

import com.buyeong.umji.api.constant.Constant
import com.buyeong.umji.api.dto.TestDto
import com.buyeong.umji.api.mapper.TestMapper
import com.buyeong.umji.api.persistence.jpa.repository.cdi.TestJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class TestJpaEntityService(
    private val testJpaRepository: TestJpaRepository,
    private val mapper: TestMapper,
) {
    fun get(id: Int): TestDto =
        testJpaRepository.findById(id).get().let {
            mapper.toDto(it)
        } ?: throw NoSuchElementException("Id($id) is not found")

    @Transactional
    fun create(dto: TestDto): TestDto {
        if (dto.id != 0) {
            throw IllegalArgumentException("ID must be zero for new staff creation")
        }
        val entity = mapper.toEntity(dto)
        return testJpaRepository.save(entity).let {
            mapper.toDto(it) ?: throw IllegalStateException("Failed to convert entity to DTO")
        }
    }

    @Transactional
    fun modify(dto: TestDto): TestDto {
        val id = dto.id
        testJpaRepository.findById(id).orElseThrow {
            NoSuchElementException("Test with id ${dto.id} not found")
        }
        val updatedEntity = mapper.toEntity(dto)

        return testJpaRepository.save(updatedEntity).let {
            mapper.toDto(it) ?: throw IllegalStateException("Failed to convert entity to DTO")
        }
    }

    @Transactional
    fun delete(id: Int) {
        testJpaRepository.deleteById(id)
    }
}