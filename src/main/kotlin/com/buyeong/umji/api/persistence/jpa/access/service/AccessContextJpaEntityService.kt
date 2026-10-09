package com.buyeong.umji.api.persistence.jpa.access.service

import com.buyeong.umji.api.domain.access.dto.AdminAccessSnapshotDto
import com.buyeong.umji.api.domain.access.dto.BuyerMembershipSnapshotDto
import com.buyeong.umji.api.domain.access.dto.ScreenPermissionMappingDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.ByteBuffer
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AccessContextJpaEntityService(
    private val jdbc: JdbcTemplate,
) {
    fun findAdminAccess(accountPublicId: UUID): AdminAccessSnapshotDto {
        val accountId = accountPublicId.toBytes()
        val roles = jdbc.queryForList(
            """SELECT role.code FROM account
                JOIN account_role ON account_role.account_id = account.id
                JOIN role ON role.id = account_role.role_id
                WHERE account.public_id = ?
            """.trimIndent(),
            String::class.java,
            accountId,
        ).toSet()
        val permissions = jdbc.queryForList(
            """SELECT DISTINCT permission.code FROM account
                JOIN account_role ON account_role.account_id = account.id
                JOIN role_permission ON role_permission.role_id = account_role.role_id
                JOIN permission ON permission.id = role_permission.permission_id
                WHERE account.public_id = ?
            """.trimIndent(),
            String::class.java,
            accountId,
        ).toSet()
        return AdminAccessSnapshotDto(roles, permissions)
    }

    fun findBuyerMembership(accountPublicId: UUID): BuyerMembershipSnapshotDto? =
        jdbc.query(
            """SELECT organization.public_id, organization.representative_account_id = account.id AS is_representative
                FROM account
                JOIN organization_member ON organization_member.account_id = account.id AND organization_member.status = 'ACTIVE'
                JOIN organization ON organization.id = organization_member.organization_id AND organization.status = 'ACTIVE'
                JOIN organization_capability ON organization_capability.organization_id = organization.id
                    AND organization_capability.capability_code = 'BUYER'
                WHERE account.public_id = ?
            """.trimIndent(),
            { result, _ ->
                BuyerMembershipSnapshotDto(
                    result.getBytes("public_id").toUuid(),
                    result.getBoolean("is_representative"),
                )
            },
            accountPublicId.toBytes(),
        ).firstOrNull()

    fun findBuyerPermissions(membershipRole: String): Set<String> =
        jdbc.queryForList(
            """SELECT permission.code FROM organization_role_permission
                JOIN permission ON permission.id = organization_role_permission.permission_id
                WHERE organization_role_permission.membership_role = ?
                ORDER BY permission.code
            """.trimIndent(),
            String::class.java,
            membershipRole,
        ).toSet()

    fun findScreens(audience: String): List<ScreenPermissionMappingDto> =
        jdbc.query(
            """SELECT ui_screen.screen_code, ui_screen.route_key, ui_screen.permission_match_mode, permission.code AS permission_code
                FROM ui_screen
                LEFT JOIN ui_screen_permission ON ui_screen_permission.ui_screen_id = ui_screen.id
                LEFT JOIN permission ON permission.id = ui_screen_permission.permission_id
                WHERE ui_screen.audience = ? AND ui_screen.active = TRUE
                ORDER BY ui_screen.display_order, ui_screen.screen_code, permission.code
            """.trimIndent(),
            { result, _ ->
                ScreenPermissionMappingDto(
                    result.getString("screen_code"),
                    result.getString("route_key"),
                    result.getString("permission_match_mode"),
                    result.getString("permission_code"),
                )
            },
            audience,
        ).groupBy { it.screenCode }
            .values
            .map { rows ->
                val first = rows.first()
                ScreenPermissionMappingDto(
                    first.screenCode,
                    first.routeKey,
                    first.permissionMatchMode,
                    null,
                    rows.mapNotNull { it.permissionCode }.toSet(),
                )
            }

    private fun UUID.toBytes(): ByteArray = ByteBuffer.allocate(16)
        .putLong(mostSignificantBits)
        .putLong(leastSignificantBits)
        .array()

    private fun ByteArray.toUuid(): UUID {
        val buffer = ByteBuffer.wrap(this)
        return UUID(buffer.long, buffer.long)
    }
}