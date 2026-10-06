package com.buyeong.umji.api.persistence.jpa.account

import com.buyeong.umji.api.operation.account.application.model.AccountData
import com.buyeong.umji.api.operation.account.application.model.BusinessProfileData
import com.buyeong.umji.api.operation.account.application.model.ConsentCommand
import com.buyeong.umji.api.operation.account.application.model.ConsentData
import com.buyeong.umji.api.operation.account.application.model.ManagedRole
import com.buyeong.umji.api.operation.account.application.model.NewAccount

import com.buyeong.umji.api.persistence.jpa.account.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.BusinessProfileEntity
import com.buyeong.umji.api.persistence.jpa.account.BuyerGroupJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.ConsentHistoryEntity
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
    private val buyerGroups: BuyerGroupJpaEntityService,
) {
    fun create(command: NewAccount): AccountData {
        val entity = accounts.save(
            AccountEntity().apply {
                name = command.name
                phone = command.phone
                phoneNormalized = command.normalizedPhone
                email = command.email
                status = "PENDING_CONSENT"
            },
        )
        command.profile?.let { saveProfile(entity, it) }
        buyerGroups.ensureForAccount(requireNotNull(entity.publicId))
        return data(entity)
    }

    @Transactional(readOnly = true)
    fun find(id: UUID): AccountData? = accounts.findByPublicId(id)?.let { data(it, true) }

    @Transactional(readOnly = true)
    fun list(status: String?, page: Int, size: Int): List<AccountData> {
        val pageable = PageRequest.of(page, size, Sort.by("id").descending())
        return (if (status.isNullOrBlank()) accounts.findAll(pageable) else accounts.findAllByStatus(status, pageable)).content.map(::data)
    }

    fun updateStatus(id: UUID, status: String): AccountData? = accounts.findByPublicId(id)?.let { entity ->
        if (entity.status != status) {
            entity.status = status
            entity.tokenVersion++
        }
        data(entity)
    }

    fun updateProfile(id: UUID, profile: BusinessProfileData, nextStatus: String): AccountData? = accounts.findByPublicId(id)?.let { entity ->
        saveProfile(entity, profile)
        entity.status = nextStatus
        data(entity)
    }

    fun assignBuyerGroup(id: UUID, buyerGroupId: UUID): AccountData? = accounts.findByPublicId(id)?.let { entity ->
        buyerGroups.assignAccountToBusinessGroup(id, buyerGroupId)
        data(entity, true)
    }

    fun setBuyerGroupRepresentative(groupId: UUID, accountId: UUID) = buyerGroups.setRepresentative(groupId, accountId)

    fun addConsent(id: UUID, consent: ConsentCommand, nextStatus: String): AccountData? = accounts.findByPublicId(id)?.let { entity ->
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

    fun approve(id: UUID): AccountData? = accounts.findByPublicId(id)?.let { entity ->
        entity.status = "ACTIVE"
        entity.tokenVersion++
        data(entity, true)
    }

    @Transactional(readOnly = true)
    fun managedRoles(): List<ManagedRole> = jdbc.query(
        "SELECT code, name FROM role WHERE code IN ('PRODUCT_MANAGER', 'ORDER_MANAGER', 'INVENTORY_MANAGER', 'SHIPPING_MANAGER') ORDER BY code",
    ) { result, _ -> ManagedRole(result.getString("code"), result.getString("name")) }

    @Transactional(readOnly = true)
    fun roles(id: UUID): List<ManagedRole>? {
        val accountId = internalAccountId(id) ?: return null
        return rolesFor(accountId)
    }

    fun grantRole(id: UUID, roleCode: String, grantedBy: UUID): List<ManagedRole>? {
        val targetId = internalAccountId(id) ?: return null
        val grantorId = internalAccountId(grantedBy) ?: throw IllegalStateException("권한 부여자를 찾을 수 없습니다.")
        val changed = jdbc.update(
            """INSERT IGNORE INTO account_role (account_id, role_id, granted_by)
                SELECT ?, r.id, ? FROM role r WHERE r.code = ? AND r.code IN ('PRODUCT_MANAGER', 'ORDER_MANAGER', 'INVENTORY_MANAGER', 'SHIPPING_MANAGER')
            """.trimIndent(),
            targetId,
            grantorId,
            roleCode,
        )
        if (changed > 0) jdbc.update("UPDATE account SET token_version = token_version + 1 WHERE id = ?", targetId)
        return rolesFor(targetId)
    }

    fun revokeRole(id: UUID, roleCode: String): List<ManagedRole>? {
        val targetId = internalAccountId(id) ?: return null
        val changed = jdbc.update(
            """DELETE ar FROM account_role ar JOIN role r ON r.id = ar.role_id
                WHERE ar.account_id = ? AND r.code = ? AND r.code IN ('PRODUCT_MANAGER', 'ORDER_MANAGER', 'INVENTORY_MANAGER', 'SHIPPING_MANAGER')
            """.trimIndent(),
            targetId,
            roleCode,
        )
        if (changed > 0) jdbc.update("UPDATE account SET token_version = token_version + 1 WHERE id = ?", targetId)
        return rolesFor(targetId)
    }

    private fun internalAccountId(publicId: UUID): Long? = accounts.findByPublicId(publicId)?.id

    private fun rolesFor(accountId: Long): List<ManagedRole> = jdbc.query(
        "SELECT r.code, r.name FROM account_role ar JOIN role r ON r.id = ar.role_id WHERE ar.account_id = ? ORDER BY r.code",
        { result, _ -> ManagedRole(result.getString("code"), result.getString("name")) },
        accountId,
    )

    private fun saveProfile(entity: AccountEntity, data: BusinessProfileData) {
        val profile = accounts.profile(requireNotNull(entity.id)) ?: BusinessProfileEntity().apply { account = entity }
        profile.businessName = data.businessName
        profile.businessRegistrationNumber = data.businessRegistrationNumber
        profile.representativeName = data.representativeName
        profile.businessPhone = data.businessPhone
        profile.postalCode = data.postalCode
        profile.address1 = data.address1
        profile.address2 = data.address2
        profile.status = data.status
        accounts.saveProfile(profile)
    }

    private fun data(entity: AccountEntity, includeConsents: Boolean = false): AccountData {
        val accountId = requireNotNull(entity.id)
        val profile = accounts.profile(accountId)?.let {
            BusinessProfileData(it.businessName, it.businessRegistrationNumber, it.representativeName, it.businessPhone, it.postalCode, it.address1, it.address2, it.status)
        }
        val consents = if (includeConsents) {
            accounts.consents(accountId).map {
                ConsentData(it.consentType, it.documentVersion, it.consentMethod, it.evidenceReference, it.processedBy?.publicId, it.consentedAt)
            }
        } else {
            emptyList()
        }
        val buyerGroupId = buyerGroups.activeForAccount(requireNotNull(entity.id))?.publicId
        return AccountData(requireNotNull(entity.publicId), entity.name, entity.phone, entity.email, entity.status, entity.tokenVersion, profile, consents, buyerGroupId)
    }
}