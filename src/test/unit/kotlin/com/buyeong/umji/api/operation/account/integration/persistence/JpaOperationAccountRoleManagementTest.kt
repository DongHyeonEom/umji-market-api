package com.buyeong.umji.api.operation.account.integration.persistence

import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.service.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OrganizationJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.service.OperationAccountJpaEntityService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import org.springframework.jdbc.core.JdbcTemplate

class JpaOperationAccountRoleManagementTest : DescribeSpec({
    val accounts = mockk<AccountJpaEntityService>(relaxed = true)
    val jdbc = mockk<JdbcTemplate>(relaxed = true)
    val adapter = OperationAccountJpaEntityService(accounts, jdbc, mockk<OrganizationJpaEntityService>(relaxed = true))
    val targetPublicId = UUID.randomUUID()
    val operatorPublicId = UUID.randomUUID()
    val targetDatabaseId = 42L
    val operatorDatabaseId = 7L

    beforeTest {
        clearMocks(accounts, jdbc)
        every { accounts.findByPublicId(targetPublicId) } returns AccountEntity().apply { id = targetDatabaseId }
        every { accounts.findByPublicId(operatorPublicId) } returns AccountEntity().apply { id = operatorDatabaseId }
    }

    describe("role 변경과 token version") {
        it("신규 role 부여 시 요청자를 기록하고 대상 token version을 갱신한다") {
            every { jdbc.update(match { it.startsWith("INSERT IGNORE INTO account_role") }, *anyVararg()) } returns 1

            adapter.grantRole(targetPublicId, "PRODUCT_MANAGER", operatorPublicId)

            verify(exactly = 1) {
                jdbc.update(
                    match { it.contains("granted_by") && it.contains("INVENTORY_MANAGER") },
                    targetDatabaseId,
                    operatorDatabaseId,
                    "PRODUCT_MANAGER",
                )
            }
            verify(exactly = 1) { jdbc.update("UPDATE account SET token_version = token_version + 1 WHERE id = ?", targetDatabaseId) }
        }

        it("중복 부여는 token version을 갱신하지 않는다") {
            every { jdbc.update(match { it.startsWith("INSERT IGNORE INTO account_role") }, *anyVararg()) } returns 0

            adapter.grantRole(targetPublicId, "PRODUCT_MANAGER", operatorPublicId)

            verify(exactly = 0) { jdbc.update("UPDATE account SET token_version = token_version + 1 WHERE id = ?", any<Long>()) }
        }

        it("실제 role 회수 시 token version을 갱신한다") {
            every { jdbc.update(match { it.startsWith("DELETE ar FROM account_role") }, *anyVararg()) } returns 1

            adapter.revokeRole(targetPublicId, "ORDER_MANAGER")

            verify(exactly = 1) { jdbc.update("UPDATE account SET token_version = token_version + 1 WHERE id = ?", targetDatabaseId) }
        }

        it("이미 회수된 role은 token version을 갱신하지 않는다") {
            every { jdbc.update(match { it.startsWith("DELETE ar FROM account_role") }, *anyVararg()) } returns 0

            adapter.revokeRole(targetPublicId, "ORDER_MANAGER")

            verify(exactly = 0) { jdbc.update("UPDATE account SET token_version = token_version + 1 WHERE id = ?", any<Long>()) }
        }

        it("존재하지 않는 계정은 role을 변경하지 않는다") {
            every { accounts.findByPublicId(targetPublicId) } returns null

            adapter.grantRole(targetPublicId, "PRODUCT_MANAGER", operatorPublicId) shouldBe null

            verify(exactly = 0) { jdbc.update(any<String>(), *anyVararg()) }
        }
    }
})
