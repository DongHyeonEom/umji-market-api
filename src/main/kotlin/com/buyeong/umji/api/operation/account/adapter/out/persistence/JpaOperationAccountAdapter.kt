package com.buyeong.umji.api.operation.account.adapter.out.persistence

import com.buyeong.umji.api.operation.account.application.model.AccountData
import com.buyeong.umji.api.operation.account.application.model.BusinessProfileData
import com.buyeong.umji.api.operation.account.application.model.ConsentCommand
import com.buyeong.umji.api.operation.account.application.model.ConsentData
import com.buyeong.umji.api.operation.account.application.model.NewAccount
import com.buyeong.umji.api.operation.account.application.port.out.OperationAccountPort
import com.buyeong.umji.api.persistence.jpa.account.AccountEntity
import com.buyeong.umji.api.persistence.jpa.account.AccountJpaEntityService
import com.buyeong.umji.api.persistence.jpa.account.BusinessProfileEntity
import com.buyeong.umji.api.persistence.jpa.account.ConsentHistoryEntity
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
@Transactional
class JpaOperationAccountAdapter(private val accounts: AccountJpaEntityService) : OperationAccountPort {
    override fun create(command: NewAccount): AccountData {
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
        return data(entity)
    }

    @Transactional(readOnly = true)
    override fun find(id: UUID): AccountData? = accounts.findByPublicId(id)?.let { data(it, true) }

    @Transactional(readOnly = true)
    override fun list(status: String?, page: Int, size: Int): List<AccountData> {
        val pageable = PageRequest.of(page, size, Sort.by("id").descending())
        return (if (status.isNullOrBlank()) accounts.findAll(pageable) else accounts.findAllByStatus(status, pageable)).content.map(::data)
    }

    override fun updateStatus(id: UUID, status: String): AccountData? = accounts.findByPublicId(id)?.let { entity ->
        if (entity.status != status) {
            entity.status = status
            entity.tokenVersion++
        }
        data(entity)
    }

    override fun updateProfile(id: UUID, profile: BusinessProfileData, nextStatus: String): AccountData? = accounts.findByPublicId(id)?.let { entity ->
        saveProfile(entity, profile)
        entity.status = nextStatus
        data(entity)
    }

    override fun addConsent(id: UUID, consent: ConsentCommand, nextStatus: String): AccountData? = accounts.findByPublicId(id)?.let { entity ->
        accounts.saveConsent(
            ConsentHistoryEntity().apply {
                account = entity
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
    override fun hasConsent(id: UUID, consentType: String): Boolean = accounts.findByPublicId(id)?.id?.let { accounts.hasConsent(it, consentType) } ?: false

    override fun approve(id: UUID): AccountData? = accounts.findByPublicId(id)?.let { entity ->
        entity.status = "ACTIVE"
        entity.tokenVersion++
        data(entity, true)
    }

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
                ConsentData(it.consentType, it.documentVersion, it.consentMethod, it.evidenceReference, it.consentedAt)
            }
        } else {
            emptyList()
        }
        return AccountData(requireNotNull(entity.publicId), entity.name, entity.phone, entity.email, entity.status, entity.tokenVersion, profile, consents)
    }
}