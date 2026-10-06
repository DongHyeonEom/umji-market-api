package com.buyeong.umji.api.operation.account.application

import com.buyeong.umji.api.persistence.jpa.account.OperationAccountJpaEntityService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID

class OperationAccountRoleManagementTest : DescribeSpec({
    val accounts = mockk<OperationAccountJpaEntityService>(relaxed = true)
    val service = OperationAccountService(accounts)
    val accountId = UUID.randomUUID()
    val operatorId = UUID.randomUUID()

    beforeTest {
        clearMocks(accounts)
    }

    describe("관리자 role 부여·회수 정책") {
        it("허용된 운영 role만 부여한다") {
            every { accounts.grantRole(accountId, "INVENTORY_MANAGER", operatorId) } returns emptyList()

            service.grantRole(accountId, "INVENTORY_MANAGER", operatorId) shouldContainExactly emptyList()

            verify(exactly = 1) { accounts.grantRole(accountId, "INVENTORY_MANAGER", operatorId) }
        }

        it("관리자와 고객 role 부여를 거부한다") {
            listOf("ADMIN", "SUPER_ADMIN", "CUSTOMER").forEach { roleCode ->
                shouldThrow<IllegalArgumentException> { service.grantRole(accountId, roleCode, operatorId) }
            }

            verify(exactly = 0) { accounts.grantRole(any(), any(), any()) }
        }

        it("관리자와 고객 role 회수를 거부한다") {
            listOf("ADMIN", "SUPER_ADMIN", "CUSTOMER").forEach { roleCode ->
                shouldThrow<IllegalArgumentException> { service.revokeRole(accountId, roleCode) }
            }

            verify(exactly = 0) { accounts.revokeRole(any(), any()) }
        }
    }
})