package com.buyeong.umji.api.persistence.jpa.account.service

import com.buyeong.umji.api.domain.operation.account.dto.AccountDataDto
import com.buyeong.umji.api.domain.operation.account.dto.ConsentCommandDto
import com.buyeong.umji.api.domain.operation.account.dto.ConsentDataDto
import com.buyeong.umji.api.domain.operation.account.dto.ManagedRoleDto
import com.buyeong.umji.api.domain.operation.account.dto.NewAccountDto
import com.buyeong.umji.api.domain.operation.account.dto.OrganizationProfileDataDto
import com.buyeong.umji.api.persistence.jpa.account.entity.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.entity.ConsentHistoryEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class OperationAccountJpaEntityService(
    private val accounts: AccountJpaEntityService,
    private val jdbc: JdbcTemplate,
    private val organizations: OrganizationJpaEntityService,
) {
    fun create(command: NewAccountDto): AccountDataDto {
        val entity = accounts.save(
            AccountEntity().apply {
                name = command.name
                phone = command.phone
                phoneNormalized = command.normalizedPhone
                email = command.email
                status = "PENDING_CONSENT"
            },
        )
        organizations.ensureForAccount(requireNotNull(entity.publicId), command.organizationCapability, command.profile)
        return data(entity)
    }

    @Transactional(readOnly = true)
    fun find(id: UUID): AccountDataDto? = accounts.findByPublicId(id)?.let { data(it, true) }

    @Transactional(readOnly = true)
    fun list(status: String?, page: Int, size: Int): List<AccountDataDto> {
        val pageable = PageRequest.of(page, size, Sort.by("id").descending())
        return (if (status.isNullOrBlank()) accounts.findAll(pageable) else accounts.findAllByStatus(status, pageable)).content.map(::data)
    }

    fun updateStatus(id: UUID, status: String): AccountDataDto? = accounts.findByPublicId(id)?.let { entity ->
        if (entity.status != status) {
            entity.status = status
            entity.tokenVersion++
        }
        data(entity)
    }

    fun updateProfile(id: UUID, profile: OrganizationProfileDataDto, nextStatus: String): AccountDataDto? = accounts.findByPublicId(id)?.let { entity ->
        organizations.updateBusinessProfileForAccount(id, profile)
        entity.status = nextStatus
        data(entity)
    }

    fun assignOrganization(id: UUID, organizationId: UUID): AccountDataDto? = accounts.findByPublicId(id)?.let { entity ->
        organizations.assignAccountToOrganization(id, organizationId)
        data(entity, true)
    }

    fun setOrganizationRepresentative(organizationId: UUID, accountId: UUID) = organizations.setRepresentative(organizationId, accountId)

    fun addConsent(id: UUID, consent: ConsentCommandDto, nextStatus: String): AccountDataDto? = accounts.findByPublicId(id)?.let { entity ->
        val processor = accounts.findByPublicId(consent.processedBy)
            ?: throw IllegalStateException("동의 처리자 계정을 찾을 수 없습니다.")
        accounts.saveConsent(
            ConsentHistoryEntity().apply {
                account = entity
                processedBy = processor
                consentType = consent.consentType
                documentVersion = consent.documentVersion
                consentMethod = consent.consentMethod
                evidenceReference = consent.evidenceReference
                consentedAt = Instant.now()
            },
        )
        entity.status = nextStatus
        data(entity, true)
    }

    @Transactional(readOnly = true)
    fun hasConsent(id: UUID, consentType: String): Boolean = accounts.findByPublicId(id)?.id?.let { accounts.hasConsent(it, consentType) } ?: false

    fun approve(id: UUID): AccountDataDto? = accounts.findByPublicId(id)?.let { entity ->
        entity.status = "ACTIVE"
        entity.tokenVersion++
        data(entity, true)
    }

    @Transactional(readOnly = true)
    fun managedRoles(): List<ManagedRoleDto> = jdbc.query(
        "SELECT code, name FROM role WHERE code IN ('PRODUCT_MANAGER', 'ORDER_MANAGER', 'INVENTORY_MANAGER', 'SHIPPING_MANAGER', 'SALES_MANAGER') ORDER BY code",
    ) { result, _ -> ManagedRoleDto(result.getString("code"), result.getString("name")) }

    @Transactional(readOnly = true)
    fun roles(id: UUID): List<ManagedRoleDto>? {
        val accountId = internalAccountId(id) ?: return null
        return rolesFor(accountId)
    }

    fun grantRole(id: UUID, roleCode: String, grantedBy: UUID): List<ManagedRoleDto>? {
        val targetId = internalAccountId(id) ?: return null
        val grantorId = internalAccountId(grantedBy) ?: throw IllegalStateException("권한 부여자를 찾을 수 없습니다.")
        val changed = jdbc.update(
            """INSERT IGNORE INTO account_role (account_id, role_id, granted_by)
                SELECT ?, r.id, ? FROM role r WHERE r.code = ? AND r.code IN ('PRODUCT_MANAGER', 'ORDER_MANAGER', 'INVENTORY_MANAGER', 'SHIPPING_MANAGER', 'SALES_MANAGER')
            """.trimIndent(),
            targetId,
            grantorId,
            roleCode,
        )
        if (changed > 0) jdbc.update("UPDATE account SET token_version = token_version + 1 WHERE id = ?", targetId)
        return rolesFor(targetId)
    }

    fun revokeRole(id: UUID, roleCode: String): List<ManagedRoleDto>? {
        val targetId = internalAccountId(id) ?: return null
        val changed = jdbc.update(
            """DELETE ar FROM account_role ar JOIN role r ON r.id = ar.role_id
                WHERE ar.account_id = ? AND r.code = ? AND r.code IN ('PRODUCT_MANAGER', 'ORDER_MANAGER', 'INVENTORY_MANAGER', 'SHIPPING_MANAGER', 'SALES_MANAGER')
            """.trimIndent(),
            targetId,
            roleCode,
        )
        if (changed > 0) jdbc.update("UPDATE account SET token_version = token_version + 1 WHERE id = ?", targetId)
        return rolesFor(targetId)
    }

    private fun internalAccountId(publicId: UUID): Long? = accounts.findByPublicId(publicId)?.id

    private fun rolesFor(accountId: Long): List<ManagedRoleDto> = jdbc.query(
        "SELECT r.code, r.name FROM account_role ar JOIN role r ON r.id = ar.role_id WHERE ar.account_id = ? ORDER BY r.code",
        { result, _ -> ManagedRoleDto(result.getString("code"), result.getString("name")) },
        accountId,
    )

    private fun data(entity: AccountEntity, includeConsents: Boolean = false): AccountDataDto {
        val accountId = requireNotNull(entity.id)
        val profile = organizations.profileForAccount(requireNotNull(entity.publicId))?.let {
            OrganizationProfileDataDto(it.businessName, it.businessRegistrationNumber, it.representativeName, it.businessPhone, it.postalCode, it.address1, it.address2, it.status)
        }
        val consents = if (includeConsents) {
            accounts.consents(accountId).map {
                ConsentDataDto(it.consentType, it.documentVersion, it.consentMethod, it.evidenceReference, it.processedBy?.publicId, it.consentedAt)
            }
        } else {
            emptyList()
        }
        val organization = organizations.activeForAccount(requireNotNull(entity.id))
        val organizationId = organization?.publicId
        val organizationCapabilities = organization?.id?.let(organizations::capabilitiesForOrganization).orEmpty()
        return AccountDataDto(
            requireNotNull(
                entity.publicId
            ),
            entity.name, entity.phone, entity.email, entity.status, entity.tokenVersion, profile, consents, organizationId, organizationCapabilities
        )
    }
}