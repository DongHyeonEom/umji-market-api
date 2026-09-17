package com.buyeong.umji.api.persistence.jpa.service.cdi

import com.buyeong.umji.api.constant.Constant
import com.buyeong.umji.api.dto.StaffDto
import com.buyeong.umji.api.mapper.StaffMapper
import com.buyeong.umji.api.persistence.jpa.repository.cdi.StaffJpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class StaffJpaEntityService(
    private val staffJpaRepository: StaffJpaRepository,
    private val mapper: StaffMapper,
) {
    fun get(id: Int): StaffDto? = staffJpaRepository.findByIdOrNull(
        id = id,
    ).let {
        mapper.toDto(it)
    }

    fun getAllByBranchId(
        branchId: Int,
    ): List<StaffDto> = staffJpaRepository.getAllByBranchId(
        branchId = branchId,
    ).let {
        mapper.toDtoList(it)
    }

    @Transactional
    fun create(dto: StaffDto): StaffDto {
        if (dto.id != 0) {
            throw IllegalArgumentException("Staff ID must be zero for new staff creation")
        }

        return staffJpaRepository.save(
            mapper.toEntity(dto),
        ).let {
            mapper.toDto(it)!!
        }
    }

    @Transactional
    fun update(dto: StaffDto): StaffDto {
        staffJpaRepository.findById(
            dto.id,
        ).orElseThrow {
            NoSuchElementException("Staff with id ${dto.id} not found")
        }

        return staffJpaRepository.save(
            mapper.toEntity(dto),
        ).let {
            mapper.toDto(it)!!
        }
    }

    @Transactional
    fun delete(staffId: Int) {
        staffJpaRepository.deleteById(staffId)
    }
}