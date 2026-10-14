package com.buyeong.umji.api.persistence.jpa.account.repository

import com.buyeong.umji.api.persistence.jpa.account.entity.BrandShippingManagerAssignmentEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface BrandShippingManagerAssignmentRepository : JpaRepository<BrandShippingManagerAssignmentEntity, Long> {
    fun findAllByBrand_PublicId(brandId: UUID): List<BrandShippingManagerAssignmentEntity>
    fun findAllByAccount_PublicId(accountId: UUID): List<BrandShippingManagerAssignmentEntity>
    fun findByBrand_PublicIdAndAccount_PublicId(brandId: UUID, accountId: UUID): BrandShippingManagerAssignmentEntity?
}
