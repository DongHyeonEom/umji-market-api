package com.buyeong.umji.api.persistence.jpa.repository.cdi

import com.buyeong.umji.api.persistence.jpa.entity.cdi.StaffJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StaffJpaRepository : JpaRepository<StaffJpaEntity, Int> {
    fun getAllByBranchId(branchId: Int): List<StaffJpaEntity>
}