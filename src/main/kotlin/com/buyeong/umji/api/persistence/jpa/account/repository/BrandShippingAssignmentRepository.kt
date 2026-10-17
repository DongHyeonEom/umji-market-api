package com.buyeong.umji.api.persistence.jpa.account.repository

import com.buyeong.umji.api.persistence.jpa.account.entity.BrandShippingAssignmentEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BrandShippingAssignmentRepository : JpaRepository<BrandShippingAssignmentEntity, Long> {
    fun findAllByBrand_PublicIdAndAssignmentType(brandId: UUID, assignmentType: String): List<BrandShippingAssignmentEntity>
    fun findAllByAccount_PublicId(accountId: UUID): List<BrandShippingAssignmentEntity>
    fun existsByAccount_PublicIdAndAssignmentType(accountId: UUID, assignmentType: String): Boolean
    fun findByBrand_PublicIdAndAccount_PublicId(brandId: UUID, accountId: UUID): BrandShippingAssignmentEntity?
    fun findByBrand_PublicIdAndAccount_PublicIdAndAssignmentType(brandId: UUID, accountId: UUID, assignmentType: String): BrandShippingAssignmentEntity?
    fun existsByBrand_PublicIdAndAccount_PublicIdAndAssignmentType(brandId: UUID, accountId: UUID, assignmentType: String): Boolean
}
