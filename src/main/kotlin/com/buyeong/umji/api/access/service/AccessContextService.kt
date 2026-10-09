package com.buyeong.umji.api.access.service

import com.buyeong.umji.api.access.model.AccessAudience
import com.buyeong.umji.api.access.model.AccessContextResponse
import com.buyeong.umji.api.access.model.AccessScreenResponse
import com.buyeong.umji.api.auth.service.CurrentAccountService
import com.buyeong.umji.api.exception.ForbiddenOperationException
import com.buyeong.umji.api.persistence.jpa.access.service.AccessContextJpaEntityService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccessContextService(
    private val currentAccounts: CurrentAccountService,
    private val accessContexts: AccessContextJpaEntityService,
) {
    @Transactional(readOnly = true)
    fun get(audience: AccessAudience): AccessContextResponse {
        val accountId = currentAccounts.activeAccountPublicId()
        val roles: Set<String>
        val permissions: Set<String>
        val organizationId: java.util.UUID?
        val membershipRole: String?

        when (audience) {
            AccessAudience.ADMIN -> {
                val access = accessContexts.findAdminAccess(accountId)
                roles = access.roles.intersect(ADMIN_ROLES)
                if (roles.isEmpty()) throw ForbiddenOperationException("운영 화면 접근 권한이 없습니다.")
                permissions = access.permissions
                organizationId = null
                membershipRole = null
            }
            AccessAudience.BUYER -> {
                val membership = accessContexts.findBuyerMembership(accountId)
                val role = when {
                    membership == null -> UNASSIGNED
                    membership.isRepresentative -> REPRESENTATIVE
                    else -> MEMBER
                }
                roles = setOf(role)
                permissions = accessContexts.findBuyerPermissions(role)
                organizationId = membership?.organizationId
                membershipRole = role
            }
        }

        val screens = accessContexts.findScreens(audience.name)
            .filter { screen -> screen.isAllowed(permissions) }
            .map { AccessScreenResponse(it.screenCode, it.routeKey) }

        return AccessContextResponse(
            audience = audience,
            roles = roles.sorted(),
            permissions = permissions.sorted(),
            organizationId = organizationId,
            membershipRole = membershipRole,
            screens = screens,
        )
    }

    private fun com.buyeong.umji.api.access.dto.ScreenPermissionMappingDto.isAllowed(permissions: Set<String>): Boolean =
        when (permissionMatchMode) {
            MATCH_ALL -> requiredPermissions.isNotEmpty() && permissions.containsAll(requiredPermissions)
            MATCH_ANY -> requiredPermissions.any(permissions::contains)
            else -> false
        }

    private companion object {
        const val UNASSIGNED = "UNASSIGNED"
        const val REPRESENTATIVE = "REPRESENTATIVE"
        const val MEMBER = "MEMBER"
        const val MATCH_ALL = "ALL"
        const val MATCH_ANY = "ANY"
        val ADMIN_ROLES = setOf("ADMIN", "SUPER_ADMIN", "PRODUCT_MANAGER", "ORDER_MANAGER", "INVENTORY_MANAGER", "SHIPPING_MANAGER", "SALES_MANAGER")
    }
}