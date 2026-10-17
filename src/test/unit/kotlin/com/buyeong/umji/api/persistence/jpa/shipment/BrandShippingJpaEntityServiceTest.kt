package com.buyeong.umji.api.persistence.jpa.shipment

import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.BrandShippingAssignmentEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.OrganizationEntity
import com.buyeong.umji.api.persistence.jpa.account.repository.BrandShippingAssignmentRepository
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OperationAccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.catalog.entity.BrandEntity
import com.buyeong.umji.api.persistence.jpa.catalog.repository.BrandShippingRateRepository
import com.buyeong.umji.api.persistence.jpa.catalog.service.CatalogJpaEntityService
import com.buyeong.umji.api.persistence.jpa.shipment.service.BrandShippingJpaEntityService
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class BrandShippingJpaEntityServiceTest : DescribeSpec({
    val catalog = mockk<CatalogJpaEntityService>(relaxed = true)
    val accounts = mockk<AccountJpaEntityService>(relaxed = true)
    val assignments = mockk<BrandShippingAssignmentRepository>(relaxed = true)
    val rates = mockk<BrandShippingRateRepository>(relaxed = true)
    val operationAccounts = mockk<OperationAccountJpaEntityService>(relaxed = true)
    val service = BrandShippingJpaEntityService(catalog, accounts, assignments, rates, operationAccounts)
    val brandId = UUID.randomUUID()
    val managerId = UUID.randomUUID()
    val operatorId = UUID.randomUUID()
    val assignerId = UUID.randomUUID()
    val brandEntity = mockk<BrandEntity> {
        every { organization } returns mockk<OrganizationEntity>()
    }
    val manager = mockk<AccountEntity> {
        every { status } returns "ACTIVE"
        every { name } returns "Manager"
    }
    val operator = mockk<AccountEntity> {
        every { status } returns "ACTIVE"
        every { name } returns "Operator"
    }
    val assigner = mockk<AccountEntity>()

    beforeTest {
        io.mockk.clearMocks(catalog, accounts, assignments, rates, operationAccounts)
        every { catalog.brand(brandId) } returns brandEntity
        every { accounts.findByPublicId(managerId) } returns manager
        every { accounts.findByPublicId(operatorId) } returns operator
        every { accounts.findByPublicId(assignerId) } returns assigner
        every { assignments.saveAndFlush(any()) } answers { firstArg() }
        every { operationAccounts.grantRole(any(), any(), any()) } returns emptyList()
        every { operationAccounts.revokeRole(any(), any()) } returns emptyList()
    }

    it("브랜드 manager 배정에서 MANAGER scope와 manager role을 함께 저장한다") {
        every { assignments.findByBrand_PublicIdAndAccount_PublicId(brandId, managerId) } returns null

        service.assignManager(brandId, managerId, assignerId)

        verify {
            assignments.saveAndFlush(match {
                it.brand === brandEntity && it.account === manager && it.assignmentType == "MANAGER"
            })
            operationAccounts.grantRole(managerId, "SHIPPING_MANAGER", assignerId)
        }
    }

    it("OPERATOR로 배정 유형을 변경하면 이전 유형이 비었을 때 이전 role을 회수한다") {
        val existing = BrandShippingAssignmentEntity().apply {
            this.brand = brandEntity
            account = manager
            assignedBy = assigner
            assignmentType = "MANAGER"
        }
        every { assignments.findByBrand_PublicIdAndAccount_PublicId(brandId, managerId) } returns existing
        every { assignments.existsByAccount_PublicIdAndAssignmentType(managerId, "MANAGER") } returns false

        service.assignOperator(brandId, managerId, assignerId)

        verify {
            assignments.saveAndFlush(match { it.assignmentType == "OPERATOR" })
            operationAccounts.revokeRole(managerId, "SHIPPING_MANAGER")
            operationAccounts.grantRole(managerId, "SHIPPING_OPERATOR", assignerId)
        }
    }

    it("마지막 브랜드 operator 배정 해제에서 operator role을 회수한다") {
        val existing = BrandShippingAssignmentEntity().apply {
            this.brand = brandEntity
            account = operator
            assignedBy = assigner
            assignmentType = "OPERATOR"
        }
        every { assignments.findByBrand_PublicIdAndAccount_PublicIdAndAssignmentType(brandId, operatorId, "OPERATOR") } returns existing
        every { assignments.existsByAccount_PublicIdAndAssignmentType(operatorId, "OPERATOR") } returns false

        service.removeOperator(brandId, operatorId)

        verify {
            assignments.delete(existing)
            assignments.flush()
            operationAccounts.revokeRole(operatorId, "SHIPPING_OPERATOR")
        }
    }
})
