package com.buyeong.umji.api.access.service

import com.buyeong.umji.api.access.model.AccessAudience
import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.persistence.jpa.access.service.AdminAccessSnapshot
import com.buyeong.umji.api.persistence.jpa.access.service.AccessContextJpaEntityService
import com.buyeong.umji.api.persistence.jpa.access.service.BuyerMembershipSnapshot
import com.buyeong.umji.api.persistence.jpa.access.service.ScreenPermissionMapping
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.util.UUID

class AccessContextServiceTest : DescribeSpec({
    val currentAccounts = mockk<CurrentAccountService>()
    val accessContexts = mockk<AccessContextJpaEntityService>()
    val service = AccessContextService(currentAccounts, accessContexts)
    val accountId = UUID.randomUUID()
    val organizationId = UUID.randomUUID()

    describe("audience-specific access context") {
        it("filters admin screens using current account permissions") {
            clearMocks(currentAccounts, accessContexts)
            every { currentAccounts.activeAccountPublicId() } returns accountId
            every { accessContexts.findAdminAccess(accountId) } returns adminAccess()
            every { accessContexts.findScreens("ADMIN") } returns listOf(
                screen("ADMIN_SHIPMENT_LIST", "SHIPMENT_READ"),
                screen("ADMIN_SALES_GROUP_LIST", "SALES_GROUP_READ"),
            )

            val result = service.get(AccessAudience.ADMIN)

            result.roles shouldContainExactly listOf("SHIPPING_MANAGER")
            result.permissions shouldContainExactly listOf("SHIPMENT_READ")
            result.screens.map { it.screenCode } shouldContainExactly listOf("ADMIN_SHIPMENT_LIST")
            result.organizationId shouldBe null
        }

        it("returns onboarding access for an account without a buyer organization") {
            clearMocks(currentAccounts, accessContexts)
            every { currentAccounts.activeAccountPublicId() } returns accountId
            every { accessContexts.findBuyerMembership(accountId) } returns null
            every { accessContexts.findBuyerPermissions("UNASSIGNED") } returns setOf("BUYER_GROUP_ONBOARDING_READ")
            every { accessContexts.findScreens("BUYER") } returns listOf(
                screen("BUYER_GROUP_ONBOARDING", "BUYER_GROUP_ONBOARDING_READ"),
                screen("BUYER_ORDERS", "BUYER_GROUP_ORDER_READ"),
            )

            val result = service.get(AccessAudience.BUYER)

            result.roles shouldContainExactly listOf("UNASSIGNED")
            result.membershipRole shouldBe "UNASSIGNED"
            result.organizationId shouldBe null
            result.screens.map { it.screenCode } shouldContainExactly listOf("BUYER_GROUP_ONBOARDING")
        }

        it("recalculates representative and member screens from the current membership") {
            clearMocks(currentAccounts, accessContexts)
            every { currentAccounts.activeAccountPublicId() } returns accountId
            every { accessContexts.findBuyerMembership(accountId) } returnsMany listOf(
                BuyerMembershipSnapshot(organizationId, true),
                BuyerMembershipSnapshot(organizationId, false),
            )
            every { accessContexts.findBuyerPermissions("REPRESENTATIVE") } returns setOf("BUYER_GROUP_ORDER_READ", "BUYER_GROUP_INVITE")
            every { accessContexts.findBuyerPermissions("MEMBER") } returns setOf("BUYER_GROUP_ORDER_READ")
            every { accessContexts.findScreens("BUYER") } returns listOf(
                screen("BUYER_ORDERS", "BUYER_GROUP_ORDER_READ"),
                screen("BUYER_GROUP_MEMBERS", "BUYER_GROUP_INVITE"),
            )

            val representative = service.get(AccessAudience.BUYER)
            val member = service.get(AccessAudience.BUYER)

            representative.membershipRole shouldBe "REPRESENTATIVE"
            representative.screens.map { it.screenCode } shouldContainExactly listOf("BUYER_ORDERS", "BUYER_GROUP_MEMBERS")
            member.membershipRole shouldBe "MEMBER"
            member.organizationId shouldBe organizationId
            member.screens.map { it.screenCode } shouldContainExactly listOf("BUYER_ORDERS")
        }
    }
})

private fun screen(code: String, requiredPermission: String) = ScreenPermissionMapping(
    screenCode = code,
    routeKey = code,
    permissionMatchMode = "ALL",
    permissionCode = null,
    requiredPermissions = setOf(requiredPermission),
)

private fun adminAccess() =
    AdminAccessSnapshot(
        roles = setOf("SHIPPING_MANAGER"),
        permissions = setOf("SHIPMENT_READ"),
    )
