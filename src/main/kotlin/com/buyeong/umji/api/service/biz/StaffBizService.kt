package com.buyeong.umji.api.service.biz

import com.buyeong.umji.api.dto.StaffDto
import com.buyeong.umji.api.persistence.jdbc.service.cdi.StaffJdbcEntityService
import com.buyeong.umji.api.persistence.jpa.service.cdi.StaffJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class StaffBizService(
    private val staffJpaEntityService: StaffJpaEntityService,
    private val staffJdbcEntityService: StaffJdbcEntityService,
) {
    fun find(id: Int): StaffDto? {
        val staffJdbcDto = staffJdbcEntityService.get(id)

        val staffJpaDto = staffJpaEntityService.get(id)

        return if (staffJdbcDto == staffJpaDto) {
            staffJdbcDto
        } else {
            staffJpaDto
        }
    }

    @Transactional
    fun add(staffDto: StaffDto): StaffDto = staffJpaEntityService.create(staffDto)

    @Transactional
    fun modify(staffDto: StaffDto): StaffDto = staffJpaEntityService.update(staffDto)

    @Transactional
    fun remove(staffId: Int) = staffJpaEntityService.delete(staffId)

    fun findAllStaffIdByBranchId(branchId: Int): List<Int> = staffJdbcEntityService.getAllStaffIdByBranchId(branchId)
}